package com.ecommerce.sportcenter.module.payment.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "advance_deposit", indexes = {
        @Index(name = "idx_adv_code", columnList = "code"),
        @Index(name = "idx_adv_customer", columnList = "customer_id"),
        @Index(name = "idx_adv_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_adv_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE advance_deposit SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class AdvanceDeposit extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // ADV-2026-0001

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "work_order_id")
    private Integer workOrderId; // cọc cho WO nào (optional)

    @Column(name = "sales_order_id")
    private Integer salesOrderId; // cọc cho SO nào (optional)

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AdvanceStatus status = AdvanceStatus.ACTIVE;

    @Column(name = "applied_invoice_id")
    private Integer appliedInvoiceId; // invoice đã cấn trừ

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
