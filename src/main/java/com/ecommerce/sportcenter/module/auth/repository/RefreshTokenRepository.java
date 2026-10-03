package com.ecommerce.sportcenter.module.auth.repository;

import com.ecommerce.sportcenter.module.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer>, JpaSpecificationExecutor<RefreshToken> {
    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findByUser_Id(int userId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(int userId);

    void deleteByExpiresAtBefore(Instant now);
}
