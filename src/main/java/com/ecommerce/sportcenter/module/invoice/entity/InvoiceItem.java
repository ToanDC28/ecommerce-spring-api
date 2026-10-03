package com.ecommerce.sportcenter.module.invoice.entity;

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
@Table(name = "invoice_item", indexes = {
        @Index(name = "idx_inv_item_invoice", columnList = "invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE invoice_item SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class InvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material; // null cho dòng labor/phụ phí

    @Column(name = "description", nullable = false, length = 500)
    private String description; // snapshot tên vật tư / "Nhân công sửa..."

    @Column(name = "qty", nullable = false)
    private long qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "discount", nullable = false)
    @Builder.Default
    private long discount = 0L;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;
}
