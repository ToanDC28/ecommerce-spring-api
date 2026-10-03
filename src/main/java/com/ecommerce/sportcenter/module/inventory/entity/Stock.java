package com.ecommerce.sportcenter.module.inventory.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.material.entity.Material;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "stock", indexes = {
        @Index(name = "idx_stock_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_stock_material", columnList = "material_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_wh_material", columnNames = {"warehouse_id", "material_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE stock SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Stock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "qty_on_hand", nullable = false)
    @Builder.Default
    private long qtyOnHand = 0L;

    @Column(name = "qty_reserved", nullable = false)
    @Builder.Default
    private long qtyReserved = 0L;
}
