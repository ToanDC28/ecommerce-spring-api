package com.ecommerce.sportcenter.module.payment.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_code", columnList = "code"),
        @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_method_date", columnList = "method, payment_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_code", columnNames = {"code"}),
        // transactionRef unique khi có giá trị (NULL không xung đột trên Postgres) — chống double-submit
        @UniqueConstraint(name = "uk_payment_txn_ref", columnNames = {"transaction_ref"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE payment SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Payment extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // PAY-2026-0001

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef; // mã CK ngân hàng; trống với tiền mặt

    @Column(name = "received_by", length = 100)
    private String receivedBy; // NV thu tiền — bắt buộc với CASH

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
