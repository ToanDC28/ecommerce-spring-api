package com.ecommerce.sportcenter.module.auth.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Server-side allowlist for refresh tokens (source of truth).
 * Only the SHA-256 hash is stored — never the raw token.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_jti", columnList = "Jti")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE refresh_tokens SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", referencedColumnName = "Id", nullable = false)
    private User user;

    @Column(name = "Jti", unique = true, nullable = false)
    private String jti;

    @Column(name = "TokenHash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "ExpiresAt", nullable = false)
    private Instant expiresAt;

    @Column(name = "Revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;
}
