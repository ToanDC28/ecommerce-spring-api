package com.ecommerce.sportcenter.module.auth.service;

import com.ecommerce.sportcenter.module.auth.repository.RevokedAccessTokenRepository;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory Bloom filter over revoked access-token JTIs.
 * <p>
 * Semantics: {@code mightContain == false} means definitely NOT revoked (fast allow,
 * no Redis/DB hit). {@code true} means MAYBE revoked and MUST be confirmed via
 * Redis, falling back to Postgres — a false positive only costs one extra lookup,
 * never a wrongful rejection.
 */
@Service
public class RevokedTokenBloomFilter {

    private final int expectedInsertions;
    private final double fpp;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile BloomFilter<String> filter;

    public RevokedTokenBloomFilter(
            @Value("${app.auth.bloom-expected-insertions:10000}") int expectedInsertions,
            @Value("${app.auth.bloom-fpp:0.01}") double fpp) {
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        this.filter = newFilter();
    }

    public boolean mightContain(String jti) {
        if (jti == null) {
            return false;
        }
        lock.readLock().lock();
        try {
            return filter.mightContain(jti);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(String jti) {
        if (jti == null) {
            return;
        }
        lock.readLock().lock();
        try {
            filter.put(jti);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Rebuilds the filter from the durable Postgres mirror (source of truth).
     * Called at startup and on a schedule so all instances converge and
     * Redis data loss never permanently blinds the filter.
     */
    public void rebuild(RevokedAccessTokenRepository repository) {
        BloomFilter<String> fresh = newFilter();
        repository.findByExpiresAtAfter(java.time.Instant.now())
                .forEach(r -> fresh.put(r.getJti()));
        lock.writeLock().lock();
        try {
            filter = fresh;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private BloomFilter<String> newFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, fpp);
    }
}
