package com.ecommerce.sportcenter.module.sales.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
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
@Table(name = "goods_issue_note", indexes = {
        @Index(name = "idx_gin_code", columnList = "code"),
        @Index(name = "idx_gin_so", columnList = "so_id"),
        @Index(name = "idx_gin_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_gin_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE goods_issue_note SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GoodsIssueNote extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // GIN-2026-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "so_id")
    private SalesOrder salesOrder; // null = bán lẻ trực tiếp tại quầy

    @Column(name = "customer_name")
    private String customerName; // snapshot hiển thị (đồng bộ từ SO hoặc Customer)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // bắt buộc khi bán trực tiếp (UI tạo khách trước)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private GoodsIssueType type = GoodsIssueType.EXPORT_SALE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GoodsIssueStatus status = GoodsIssueStatus.DRAFT;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "goodsIssueNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsIssueItem> items = new ArrayList<>();
}
