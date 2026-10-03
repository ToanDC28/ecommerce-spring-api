package com.ecommerce.sportcenter.module.invoice.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
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
@Table(name = "invoice", indexes = {
        @Index(name = "idx_invoice_code", columnList = "code"),
        @Index(name = "idx_invoice_type_status", columnList = "type, status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_invoice_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE invoice SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Invoice extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // INV-2026-00001

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InvoiceType type;

    @Column(name = "work_order_id")
    private Integer workOrderId; // WORK

    @Column(name = "work_order_code", length = 50)
    private String workOrderCode;

    @Column(name = "supplier_id")
    private Integer supplierId; // PURCHASE

    @Column(name = "supplier_name")
    private String supplierName; // snapshot

    @Column(name = "so_id")
    private Integer soId; // SALES

    @Column(name = "so_code", length = 50)
    private String soCode;

    @Column(name = "ref_code", length = 50)
    private String refCode; // GRN/GIN code gốc (PURCHASE/SALES)

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_id")
    private Integer customerId; // link Customer master khi có (null = khách vãng lai snapshot)

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sub_total", nullable = false)
    @Builder.Default
    private long subTotal = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private long discountAmount = 0L;

    @Column(name = "vat_rate", nullable = false)
    @Builder.Default
    private int vatRate = 10; // 0, 8, 10

    @Column(name = "vat_amount", nullable = false)
    @Builder.Default
    private long vatAmount = 0L;

    @Column(name = "grand_total", nullable = false)
    @Builder.Default
    private long grandTotal = 0L;

    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    private long paidAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();
}
