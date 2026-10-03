package com.ecommerce.sportcenter.module.workorder.entity;

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
@Table(name = "work_order_material", indexes = {
        @Index(name = "idx_wom_wo", columnList = "work_order_id"),
        @Index(name = "idx_wom_material", columnList = "material_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE work_order_material SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class WorkOrderMaterial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "qty_planned", nullable = false)
    private long qtyPlanned;

    @Column(name = "qty_actual", nullable = false)
    @Builder.Default
    private long qtyActual = 0L;

    @Column(name = "unit_cost", nullable = false)
    private long unitCost; // snapshot giá vốn lúc xuất

    @Column(name = "unit_sell_price")
    private Long unitSellPrice; // đơn giá tính cho khách

    @Column(name = "note")
    private String note;
}
