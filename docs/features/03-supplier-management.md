# 03 — Supplier Management (NCC vật tư cơ khí)

## Purpose

Manage vật tư suppliers (cửa hàng thép, đại lý phụ tùng, hãng dầu mỡ) for purchasing and debt tracking.
Suppliers là bảng `suppliers` RIÊNG — NOT `User`, NO login / NO `CLIENT` role. ADMIN nhập và duy trì.

## Current state

Done phase 1 — `module/supplier/*` (`code, name, taxCode, phone, email, address, paymentTerm, active, currentDebt`).

## Data model

`Supplier(id, code UNIQUE [SUP-001], name, taxCode?, phone, email, address, paymentTerm[PREPAID, NET_30, NET_60], isActive, currentDebt)`
`SupplierContact(supplier_id, name, phone, role)` — optional, multiple contacts per supplier (phase 2).

Notes:
- `code` auto-generated as `SUP-001, SUP-002, ...` when blank.
- `isActive` defaults `true`; `currentDebt` defaults `0` (VND, `long`, no decimals).
- No `DELETE` endpoint in phase 1 — suppliers with history are only deactivated, never hard-deleted.

## API (phase 1 — ADMIN manages supplier info)

```
GET   /api/suppliers?keyword=&isActive=&page&size   # SUPPLIER_READ
GET   /api/suppliers/{id}                           # SUPPLIER_READ
POST  /api/suppliers                                # SUPPLIER_WRITE (admin adds supplier)
PUT   /api/suppliers/{id}                           # SUPPLIER_WRITE (admin edits supplier)
PATCH /api/suppliers/{id}/active                    # SUPPLIER_WRITE (activate/deactivate)
```

Phase 2 (needs 04 Purchasing + 07 Invoice):

```
GET  /api/suppliers/{id}/purchases  # PO history (04)
GET  /api/suppliers/{id}/debt       # from unpaid PURCHASE invoices (07)
```

## RBAC

- `SUPPLIER_READ`: `ADMIN, WAREHOUSE_STAFF, ACCOUNTANT` (sales read-only).
- `SUPPLIER_WRITE`: `ADMIN` — chỉ chủ thêm/sửa/khóa NCC.

## Acceptance criteria

- [x] ADMIN can add supplier via `POST /api/suppliers` with `name` (+ optional `taxCode, phone, email, address, paymentTerm`).
- [x] `code` auto-generated if blank (`SUP-` + sequence); duplicate `code` rejected.
- [x] `GET /api/suppliers` supports `keyword` (matches `code/name/phone`) + `isActive` filter + pagination.
- [x] Cannot hard-delete supplier with history; only deactivate via `PATCH /api/suppliers/{id}/active` (soft-delete `@SQLDelete`).
- [ ] Debt = sum(`grandTotal - paidAmount`) over `PURCHASE` invoices `ISSUED/PARTIAL/OVERDUE` (có ở `GET /api/reports/supplier-debt`; endpoint riêng `/suppliers/{id}/debt` + purchase history để phase 2).

## Dependencies

Required by Purchasing/Import (04) and Purchase Invoice (07).
