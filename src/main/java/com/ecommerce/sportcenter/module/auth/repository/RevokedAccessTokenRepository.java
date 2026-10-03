package com.ecommerce.sportcenter.module.auth.repository;

import com.ecommerce.sportcenter.module.auth.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, Integer>, JpaSpecificationExecutor<RevokedAccessToken> {
    Optional<RevokedAccessToken> findByJti(String jti);

    boolean existsByJti(String jti);

    List<RevokedAccessToken> findByExpiresAtAfter(Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
