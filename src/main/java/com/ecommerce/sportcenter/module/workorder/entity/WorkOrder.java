package com.ecommerce.sportcenter.module.workorder.entity;

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
@Table(name = "work_order", indexes = {
        @Index(name = "idx_wo_code", columnList = "code"),
        @Index(name = "idx_wo_status", columnList = "status"),
        @Index(name = "idx_wo_contract", columnList = "contract_no")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wo_code", columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE work_order SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class WorkOrder extends BaseEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code; // WO-2026-0001

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WorkOrderType type;

    @Column(name = "contract_no", length = 50)
    private String contractNo;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // bắt buộc từ Customer master (giữ customerName snapshot hiển thị)

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "machine_info", columnDefinition = "TEXT")
    private String machineInfo;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    @Column(name = "labor_cost", nullable = false)
    @Builder.Default
    private long laborCost = 0L;

    @Column(name = "overhead_cost", nullable = false)
    @Builder.Default
    private long overheadCost = 0L;

    @Column(name = "agreed_price")
    private Long agreedPrice;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkOrderMaterial> materials = new ArrayList<>();
}
