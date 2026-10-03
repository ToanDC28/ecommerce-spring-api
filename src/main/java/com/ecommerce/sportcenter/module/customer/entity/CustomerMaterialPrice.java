package com.ecommerce.sportcenter.module.customer.entity;

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
@Table(name = "customer_material_price", indexes = {
        @Index(name = "idx_cmp_customer", columnList = "customer_id"),
        @Index(name = "idx_cmp_material", columnList = "material_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cmp_customer_material", columnNames = {"customer_id", "material_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE customer_material_price SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CustomerMaterialPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "sell_price", nullable = false)
    private long sellPrice; // giá bán riêng cho khách này (thường = giá sỉ thợ)
}
