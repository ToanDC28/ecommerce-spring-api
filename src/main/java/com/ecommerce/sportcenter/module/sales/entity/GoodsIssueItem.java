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
@Table(name = "goods_issue_item", indexes = {
        @Index(name = "idx_gii_gin", columnList = "gin_id"),
        @Index(name = "idx_gii_material", columnList = "material_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE goods_issue_item SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GoodsIssueItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gin_id", nullable = false)
    private GoodsIssueNote goodsIssueNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "qty", nullable = false)
    private long qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice; // snapshot giá bán lúc xuất

    @Column(name = "line_total", nullable = false)
    private long lineTotal;
}
