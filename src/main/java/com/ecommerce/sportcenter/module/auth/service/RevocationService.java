package com.ecommerce.sportcenter.module.auth.service;

import com.ecommerce.sportcenter.module.auth.entity.RevokedAccessToken;
import com.ecommerce.sportcenter.module.auth.repository.RevokedAccessTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Access-token revocation with three layers:
 * <ol>
 *   <li>Local Guava Bloom filter — negative means definitely not revoked (no I/O).</li>
 *   <li>Redis denylist mirror ({@code revoked:jti}) — fast confirm path.</li>
 *   <li>Postgres {@code revoked_access_tokens} — source of truth and fallback
 *       when Redis is unreachable, so Redis data loss never resurrects tokens.</li>
 * </ol>
 * Fail-closed: if Postgres itself is unreachable the request fails with 500
 * rather than silently accepting a possibly-revoked token.
 */
@Service
public class RevocationService {

    static final String REDIS_KEY_PREFIX = "revoked:";

    private final RevokedTokenBloomFilter bloomFilter;
    private final StringRedisTemplate redisTemplate;
    private final RevokedAccessTokenRepository revokedRepository;

    public RevocationService(RevokedTokenBloomFilter bloomFilter,
                             StringRedisTemplate redisTemplate,
                             RevokedAccessTokenRepository revokedRepository) {
        this.bloomFilter = bloomFilter;
        this.redisTemplate = redisTemplate;
        this.revokedRepository = revokedRepository;
    }

    /**
     * @return true if the access-token JTI is revoked.
     */
    @Transactional(readOnly = true)
    public boolean isAccessRevoked(String jti) {
        if (jti == null || !bloomFilter.mightContain(jti)) {
            return false;
        }
        // Bloom positive: confirm via Redis, falling back to Postgres.
        try {
            Boolean present = redisTemplate.hasKey(REDIS_KEY_PREFIX + jti);
            if (present) {
                return true;
            }
            // Key absent in Redis: either false positive or Redis lost data.
            // Postgres mirror is the source of truth.
            return revokedRepository.existsByJti(jti);
        } catch (RuntimeException redisDown) {
            return revokedRepository.existsByJti(jti);
        }
    }

    /**
     * Revokes an access token until {@code expiresAt}.
     */
    @Transactional
    public void revokeAccess(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null || expiresAt.isBefore(Instant.now())) {
            return;
        }
        revokedRepository.findByJti(jti).orElseGet(() ->
                revokedRepository.save(RevokedAccessToken.builder()
                        .jti(jti)
                        .expiresAt(expiresAt)
                        .build()));
        bloomFilter.add(jti);
        try {
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            if (!ttl.isNegative() && !ttl.isZero()) {
                redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + jti, "1", ttl);
            }
        } catch (RuntimeException redisDown) {
            // Postgres mirror already persisted; Redis will be rebuilt from it.
        }
    }

    /**
     * Rebuilds Redis + Bloom from the Postgres mirror. Called at startup and
     * on a schedule so Redis data loss self-heals.
     */
    @Transactional(readOnly = true)
    public void rebuildCaches() {
        Instant now = Instant.now();
        for (var revoked : revokedRepository.findByExpiresAtAfter(now)) {
            bloomFilter.add(revoked.getJti());
            try {
                Duration ttl = Duration.between(now, revoked.getExpiresAt());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + revoked.getJti(), "1", ttl);
                }
            } catch (RuntimeException redisDown) {
                // Keep going: Postgres remains the source of truth.
            }
        }
    }
}
