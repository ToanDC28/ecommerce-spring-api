package com.ecommerce.sportcenter.module.auth.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
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
 * Durable mirror of the Redis access-token denylist (source of truth).
 * Lets the denylist survive Redis data loss: Redis is rebuilt from this table.
 * Rows are purged once past {@code expiresAt}.
 */
@Entity
@Table(name = "revoked_access_tokens", indexes = {
        @Index(name = "idx_revoked_jti", columnList = "Jti")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE revoked_access_tokens SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RevokedAccessToken extends BaseEntity {

    @Column(name = "Jti", unique = true, nullable = false)
    private String jti;

    @Column(name = "ExpiresAt", nullable = false)
    private Instant expiresAt;
}
