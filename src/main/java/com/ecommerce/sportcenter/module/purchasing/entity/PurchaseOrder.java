package com.ecommerce.sportcenter.module.purchasing.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order", indexes = {
        @Index(name = "idx_po_code", columnList = "code"),
        @Index(name = "idx_po_supplier", columnList = "supplier_id"),
        @Index(name = "idx_po_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_po_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE purchase_order SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // PO-2026-0001

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private long totalAmount = 0L;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
