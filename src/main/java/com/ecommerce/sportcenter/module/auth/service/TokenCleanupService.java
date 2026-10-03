package com.ecommerce.sportcenter.module.auth.service;

import com.ecommerce.sportcenter.module.auth.repository.RefreshTokenRepository;
import com.ecommerce.sportcenter.module.auth.repository.RevokedAccessTokenRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled maintenance for token revocation state:
 * <ul>
 *   <li>Purges expired {@code refresh_tokens} and {@code revoked_access_tokens} rows
 *       so both tables stay tiny.</li>
 *   <li>Rebuilds the Bloom filter (+ Redis mirror) from the Postgres source of truth,
 *       so all instances converge and Redis data loss self-heals.</li>
 * </ul>
 */
@Service
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final RevokedTokenBloomFilter bloomFilter;
    private final RevocationService revocationService;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                               RevokedAccessTokenRepository revokedAccessTokenRepository,
                               RevokedTokenBloomFilter bloomFilter,
                               RevocationService revocationService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.bloomFilter = bloomFilter;
        this.revocationService = revocationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuildOnStartup() {
        rebuildCaches();
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void purgeExpired() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteByExpiresAtBefore(now);
        revokedAccessTokenRepository.deleteByExpiresAtBefore(now);
    }

    @Scheduled(fixedDelayString = "${app.auth.bloom-rebuild-ms:3600000}")
    public void rebuildCaches() {
        bloomFilter.rebuild(revokedAccessTokenRepository);
        revocationService.rebuildCaches();
    }
}
