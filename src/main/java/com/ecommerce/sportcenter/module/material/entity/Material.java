package com.ecommerce.sportcenter.module.material.entity;

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
@Table(name = "material", indexes = {
        @Index(name = "idx_material_sku", columnList = "sku"),
        @Index(name = "idx_material_name", columnList = "name"),
        @Index(name = "idx_material_category", columnList = "category_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_material_sku", columnNames = {"sku"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE material SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Material extends BaseEntity {

    @Column(name = "sku", nullable = false, length = 50)
    private String sku; // e.g. VT-THEP-CT3-10MM

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "brand", length = 100)
    private String brand; // hãng phụ tùng, optional text

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    @Builder.Default
    private MaterialUnit unit = MaterialUnit.CAI;

    @Column(name = "cost_price", nullable = false)
    @Builder.Default
    private long costPrice = 0L; // giá vốn VND

    @Column(name = "sell_price")
    private Long sellPrice; // giá bán lẻ, null = chỉ dùng nội bộ

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private long stockQty = 0L; // chỉ đọc, update qua transaction

    @Column(name = "min_stock", nullable = false)
    @Builder.Default
    private long minStock = 0L;

    @Column(name = "location", length = 50)
    private String location; // kệ A1...

    @Column(name = "image_url")
    private String imageUrl; // optional

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
