package com.ecommerce.sportcenter.module.sales.entity;

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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order", indexes = {
        @Index(name = "idx_so_code", columnList = "code"),
        @Index(name = "idx_so_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_so_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE sales_order SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class SalesOrder extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // SO-2026-0001

    @Column(name = "customer_name", nullable = false)
    private String customerName; // snapshot hiển thị (đồng bộ từ Customer khi có link)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // optional — khách vãng lai không cần mã

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SalesOrderStatus status = SalesOrderStatus.PENDING;

    @Column(name = "sub_total", nullable = false)
    @Builder.Default
    private long subTotal = 0L;

    @Column(name = "discount", nullable = false)
    @Builder.Default
    private long discount = 0L;

    @Column(name = "grand_total", nullable = false)
    @Builder.Default
    private long grandTotal = 0L;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SalesOrderItem> items = new ArrayList<>();
}
