package com.ecommerce.sportcenter.module.inventory.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "warehouse", indexes = {
        @Index(name = "idx_warehouse_code", columnList = "code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE warehouse SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Warehouse extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // WH-XUONG-01

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
