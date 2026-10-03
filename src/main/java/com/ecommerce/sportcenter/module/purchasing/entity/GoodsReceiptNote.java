package com.ecommerce.sportcenter.module.purchasing.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
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
@Table(name = "goods_receipt_note", indexes = {
        @Index(name = "idx_grn_code", columnList = "code"),
        @Index(name = "idx_grn_po", columnList = "po_id"),
        @Index(name = "idx_grn_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_grn_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE goods_receipt_note SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GoodsReceiptNote extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // GRN-2026-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id")
    private PurchaseOrder purchaseOrder; // null = mua trực tiếp không qua PO

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier; // bắt buộc khi po_id null (mua trực tiếp)

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    @Builder.Default
    private GoodsReceiptType type = GoodsReceiptType.IMPORT_PURCHASE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "goodsReceiptNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptItem> items = new ArrayList<>();
}
