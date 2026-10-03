package com.ecommerce.sportcenter.module.sales.entity;

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
@Table(name = "sales_order_item", indexes = {
        @Index(name = "idx_soi_so", columnList = "sales_order_id"),
        @Index(name = "idx_soi_material", columnList = "material_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE sales_order_item SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class SalesOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "qty", nullable = false)
    private long qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice; // snapshot Material.sellPrice lúc tạo SO

    @Column(name = "discount", nullable = false)
    @Builder.Default
    private long discount = 0L;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    @Column(name = "issued_qty", nullable = false)
    @Builder.Default
    private long issuedQty = 0L;

    @Column(name = "returned_qty", nullable = false)
    @Builder.Default
    private long returnedQty = 0L;
}
