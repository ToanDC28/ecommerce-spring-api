package com.ecommerce.sportcenter.module.purchasing.entity;

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
@Table(name = "purchase_order_item", indexes = {
        @Index(name = "idx_poi_po", columnList = "purchase_order_id"),
        @Index(name = "idx_poi_material", columnList = "material_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE purchase_order_item SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PurchaseOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "qty", nullable = false)
    private long qty;

    @Column(name = "unit_cost", nullable = false)
    private long unitCost;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    @Column(name = "received_qty", nullable = false)
    @Builder.Default
    private long receivedQty = 0L;
}
