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
@Table(name = "stock_transaction", indexes = {
        @Index(name = "idx_stx_material", columnList = "material_id"),
        @Index(name = "idx_stx_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_stx_ref", columnList = "ref_type, ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE stock_transaction SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StockTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private StockMoveType type; // IN / OUT

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 20)
    private StockRefType refType; // GRN, GIN, WORK_ORDER, ...

    @Column(name = "ref_id", nullable = false, length = 50)
    private String refId; // code PO/GRN/GIN/WO

    @Column(name = "qty_before", nullable = false)
    private long qtyBefore;

    @Column(name = "qty_change", nullable = false)
    private long qtyChange; // + nhập, - xuất

    @Column(name = "qty_after", nullable = false)
    private long qtyAfter;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
