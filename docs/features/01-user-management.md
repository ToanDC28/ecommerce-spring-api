# 01 — User Management (Staff only, internal app)

## Purpose

Manage internal staff accounts on top of the existing RBAC. Distinguish who can sell, stock, account, and administer.
There is NO public self-registration and NO `CLIENT` role: this app is for internal use only.

Suppliers are NOT users — they live in a separate `suppliers` table managed by ADMIN (see `03-supplier-management.md`).

## Current state

- `entity/User.java` (`users` + `user_roles`): `username`, `email`, `password` (BCrypt), `fullName`, `enabled`, `roles` (EAGER ManyToMany). Implements `UserDetails`.
- `entity/Permission.java`: `permissions` table entity (`name UNIQUE, description`); `Role.permissions` is `@ManyToMany` via `role_permissions`.
- `security/SecurityConfig.java` (permits only `POST /api/auth/login` + `POST /api/auth/refresh`), `JwtService.java` (access + refresh with `email/roles/permissions` claims), `JwtAuthenticationFilter.java` (accepts access tokens only), `CustomUserDetailsService.java`.
- `controller/AuthController.java`: `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/me`.
- `controller/UserController.java`: `POST /api/users` (`USER_WRITE`), `GET /api/users`, `GET /api/users/roles`, `GET /api/users/{id}`, `PUT /api/users/{id}/roles`, `PATCH /api/users/{id}/enabled`, `DELETE /api/users/{id}`.
- `config/DataSeeder.java`: seeds 18 permissions + 4 roles (`ADMIN` chủ + staff, no MANAGER, no CLIENT) + `admin / Admin@123`.

## Decision (internal-use + RBAC rework)

1. REMOVE `POST /api/auth/register` and `request/RegisterRequest.java` — no public registration.
2. REMOVE `CLIENT` and `MANAGER` roles from `DataSeeder` and from all docs. Roles only:
   `ADMIN` (chủ, full quyền), staff: `SALES_STAFF` (bán kiêm kho: ORDER + CUSTOMER + INVENTORY + INVOICE),
   `WAREHOUSE_STAFF` (kho: INVENTORY), `ACCOUNTANT` (kế toán: INVOICE + PAYMENT + PAYROLL).
3. Staff accounts are created by ADMIN via `POST /api/users` (`USER_WRITE`).
4. `SecurityConfig` only permits `POST /api/auth/login` and `POST /api/auth/refresh` (+ docs) anonymously; everything else requires JWT.
5. Supplier info is entered by ADMIN in the `suppliers` table, never as a `User` row.
6. REPLACE `Permission` enum with a `permissions` TABLE:
   - `Permission(id, name UNIQUE, description)` — new `entity/Permission.java` as `@Entity` (`permissions`).
   - `Role.permissions` becomes `@ManyToMany(fetch = EAGER)` via `role_permissions(roleId, permissionId)`.
   - `User.getAuthorities()` returns `ROLE_<name>` + each permission's `name`.
   - Controllers keep using `@PreAuthorize("hasAuthority('PRODUCT_WRITE')")` — names unchanged.
   - `PermissionRepository` added (`findByName`).
7. JWT = access token + refresh token (stateless signed JWTs, no extra table in phase 1):
   - `POST /api/auth/login` returns `{ accessToken, refreshToken, tokenType: "Bearer", username, email, roles[], permissions[] }`.
   - Access-token claims: `sub=username, email, roles[], permissions[], type="access"`, expiry `app.jwt.access-expiration-ms` (default 15m).
   - Refresh-token claims: `sub=username, type="refresh"`, expiry `app.jwt.refresh-expiration-ms` (default 7d).
   - `POST /api/auth/refresh { refreshToken }` validates `type="refresh"` + expiry + user still `enabled`, then rotates both tokens.
   - `JwtService` API: `generateAccessToken(User)`, `generateRefreshToken(User)`, `extractUsername`, `extractClaim`, `isAccessTokenValid`, `isRefreshTokenValid`.
   - Config keys: `app.jwt.secret`, `app.jwt.access-expiration-ms`, `app.jwt.refresh-expiration-ms` (keep legacy `app.jwt.expiration-ms` as fallback for access).
8. `DataSeeder` seeds permissions + roles on startup (idempotent via `findByName`):
   - 18 permissions: `PRODUCT_READ/WRITE, INVENTORY_READ/WRITE, SUPPLIER_READ/WRITE, ORDER_READ/WRITE, CUSTOMER_READ/WRITE, INVOICE_READ/WRITE, PAYMENT_MANAGE, PAYROLL_READ/WRITE, USER_READ/WRITE, ROLE_MANAGE` (each with description).
   - 4 roles: ADMIN = all 18; staff subsets as above (SALES_STAFF bán kiêm kho nên có thêm CUSTOMER_WRITE + INVENTORY_WRITE).
   - Default admin `admin / Admin@123` (`ADMIN` role) if absent.
   - Existing deployments: one-time migration drops `role_permissions.permission` enum column in favour of `permissionId` FK; stale `CLIENT`/`MANAGER` roles left untouched in old DBs (admin can delete manually).
9. Token revocation — PROPOSED (not yet implemented, this section is the spec):
   - Problem: stateless JWTs stay valid until expiry after logout. Fix with server-side state:
     Postgres `refresh_tokens` allowlist (source of truth) + Redis access-token denylist
     (Postgres mirror as source of truth) + local Guava BloomFilter in front of the denylist check.
   - Claims: both tokens carry a unique `jti`. Access: `sub, email, roles[], permissions[], type="access", jti`,
     expiry `app.jwt.access-expiration-ms` (tighten to 5m). Refresh: `sub, type="refresh", jti`, expiry 7d.
   - Tables (PostgreSQL, source of truth):
     - `refresh_tokens(id, user_id FK, jti UNIQUE, token_hash, expires_at, revoked, created_at)` —
       store SHA-256 hash, never the raw token.
     - `revoked_access_tokens(jti PK, expires_at)` — durable mirror of the Redis denylist.
   - Redis (fast path, NOT source of truth): `revoked:<jti> -> 1` with TTL = token's remaining lifetime.
     `docker/docker-compose.yml` already provides `ecommerce-redis`; app adds `spring-data-redis` (+ Guava for BloomFilter).
   - BloomFilter (Guava, in-memory per instance): built at startup from non-expired `revoked_access_tokens`,
     updated on every local revoke, rebuilt periodically. Semantics: negative = definitely not revoked (allow,
     no Redis/DB hit); positive = maybe revoked (MUST confirm via Redis, falling back to Postgres).
     Defaults: expected insertions = active revoked count, false-positive rate 1%.
   - Request path (`JwtAuthenticationFilter`): verify signature + `type="access"` + expiry →
     Bloom check on `jti` → if positive, confirm via Redis → on Redis miss/error, confirm via Postgres →
     hit = 401, miss = allow (false positive absorbed by the confirm step).
   - Flows:
     - Login: insert refresh row `{jti, hash, expires_at, revoked=false}`.
     - Refresh: look up refresh row by `jti`/hash; missing/revoked/expired → 401. Valid → revoke old row
       (rotation), insert new row, return new pair. Reuse of an already-rotated token → 401 + revoke all
       user refresh rows (theft detection).
     - Logout (`POST /api/auth/logout`, auth): revoke own refresh row + insert access `jti` into
       `revoked_access_tokens` and Redis (TTL = remaining access lifetime) + add to local Bloom.
     - Logout-all / password change / admin disable: revoke ALL refresh rows of the user + (optionally)
       rely on short access expiry for access tokens.
   - Redis-loss safety (denylist in Redis is only a cache): on startup and on Redis failure, rebuild
     Redis + Bloom from Postgres `revoked_access_tokens`. While Redis is unreachable, the filter falls back
     to Postgres (fail-closed on Postgres error). Scheduled jobs purge expired rows from both tables.
   - Config: `app.jwt.access-expiration-ms` (5m), `app.jwt.refresh-expiration-ms` (7d),
     `app.auth.bloom-expected-insertions`, `app.auth.bloom-fpp: 0.01`, `app.auth.bloom-rebuild-ms`,
     `spring.data.redis.host/port`. Postgres driver added alongside MySQL (set `spring.datasource`
     to your PostgreSQL URL + `org.hibernate.dialect.PostgreSQLDialect`).

## TODO

1. `Permission` entity + `PermissionRepository`; `Role` → `@ManyToMany Permission`. — DONE
2. `JwtService` access/refresh + claims (`email, roles, permissions`). — DONE
3. `AuthResponse { accessToken, refreshToken, ... }` + `RefreshRequest` + `POST /api/auth/refresh`. — DONE
4. `POST /api/users` (`USER_WRITE`): create staff user with `username, email, password, fullName, roles[]`. — DONE
5. `StaffProfile` (`user_id` PK/FK, future): `position`, `hireDate`, `bankAccount`, `salaryGrade_id`, `warehouse_id?`. — DEFERRED to HR/Payroll (09); not needed for workshop phase 1.
6. Token revocation (§9): `RefreshToken` + `RevokedAccessToken` entities/repos, `jti` claims, Redis denylist
   + Guava BloomFilter service, `POST /api/auth/logout`, rotation + reuse detection, cleanup jobs. — DONE
7. Change password for self (revokes all user refresh rows) — DONE (`POST /api/auth/change-password`).
   Forgot/reset via email (token + expiry) — DEFERRED: internal app (vài staff), ADMIN resets via `POST /api/users/{id}/reset-password` instead; no mail infra.
7. Admin user search: `GET /api/users?keyword&role&enabled&page&size`. — DONE (`SearchUserRequest.role` filters `roles.name` join, distinct).
8. Prevent last-admin disable/delete; prevent self-disable. — DONE (self-disable/self-delete + demote-last-ADMIN + disable/delete-last-ADMIN all return 400; `UserRepository.countOtherEnabledAdmins`).

## API

```
POST /api/auth/login                        # public -> { accessToken, refreshToken, username, email, roles[], permissions[] }
POST /api/auth/refresh                      # public, body { refreshToken } -> rotated pair (401 if invalid/expired/disabled/revoked)
POST /api/auth/logout                       # auth: revokes refresh + denylists current access jti -> 204
POST /api/auth/logout-all                   # auth: revokes all user refresh rows -> 204
GET  /api/auth/me                           # auth -> UserResponse
POST /api/auth/change-password              # auth, body { oldPassword, newPassword } -> 200 + revokes all sessions

POST  /api/users                            # USER_WRITE (admin creates staff)
GET   /api/users?keyword=&role=&enabled=&page&size   # USER_READ (role = ADMIN|SALES_STAFF|WAREHOUSE_STAFF|ACCOUNTANT)
GET   /api/users/roles                      # USER_READ
GET   /api/users/{id}                       # USER_READ
PUT   /api/users/{id}/roles                 # USER_WRITE (demoting last ADMIN -> 400)
PATCH /api/users/{id}/enabled               # USER_WRITE (self-disable / last-ADMIN disable -> 400; disable revokes sessions)
DELETE /api/users/{id}                      # USER_WRITE (self-delete / last-ADMIN delete -> 400)
POST  /api/users/{id}/reset-password        # USER_WRITE, body { newPassword } -> 200 + revokes all sessions
```

## RBAC

- `USER_READ`: list/view users, roles.
- `USER_WRITE`: create staff, assign roles, enable/disable, delete, edit profiles.
- `ROLE_MANAGE`: create custom roles (future).
- Permission names enforced via `hasAuthority('<NAME>')`; roles seed the same 18 names into the `permissions` table.
- No self-registration endpoint exists, so privilege escalation via register is impossible.

## Acceptance criteria

- [x] No `Permission` enum; `permissions` table holds 18 rows after boot.
- [x] No `CLIENT`/`MANAGER` role in DB seed; `GET /api/users/roles` returns ADMIN + 3 staff roles only.
- [x] `POST /api/auth/register` returns 404/403 (endpoint removed).
- [x] `POST /api/auth/login` returns `accessToken + refreshToken`; decoded access token contains `email, roles[], permissions[]`.
- [x] `POST /api/auth/refresh` with valid refresh token returns a new pair; expired/invalid/disabled/revoked user gets 401.
- [x] Logged-out access token rejected (401) via Bloom → Redis → Postgres denylist path;
  logged-out refresh token rejected (401) via `refresh_tokens` allowlist.
- [x] Redis data loss does NOT resurrect revoked tokens — rebuild from Postgres mirror.
- [x] Bloom false positives never lock users out (positive always confirmed before rejecting).
- [x] Refresh reuse (replay of rotated token) → 401 + all user sessions revoked.
- [x] Admin disable revokes all user refresh rows; access tokens expire within 5 minutes.
- [x] ADMIN can create `SALES_STAFF` etc. via `POST /api/users`.
- [x] ADMIN can change roles via `PUT /api/users/{id}/roles` (demoting last ADMIN → 400).
- [x] Disabled user cannot login or refresh (401).
- [x] `GET /api/users` paginated, filtered by keyword + role + enabled.
- [x] Self-disable / self-delete → 400; last-ADMIN disable/delete/demote → 400.
- [x] `POST /api/auth/change-password` verifies old password, revokes all sessions.
- [x] `POST /api/users/{id}/reset-password` (ADMIN) sets new password + revokes sessions.
- [ ] `StaffProfile` — deferred to HR/Payroll (09).
- [ ] Forgot/reset via email — deferred (no mail infra; ADMIN reset covers internal use).
- [x] Supplier data lives only in `suppliers`, not in `users`.

## Dependencies

None. This module is the foundation for `createdBy` audit on GRN/GIN/Invoice/Payroll.
