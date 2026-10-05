package com.ecommerce.sportcenter.module.payroll.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import com.ecommerce.sportcenter.module.user.entity.User;
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
@Table(name = "payroll", indexes = {
        @Index(name = "idx_payroll_period", columnList = "period"),
        @Index(name = "idx_payroll_staff", columnList = "staff_id"),
        @Index(name = "idx_payroll_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payroll_staff_period", columnNames = {"staff_id", "period"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE payroll SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Payroll extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "period", nullable = false, length = 7)
    private String period; // 2026-09

    // Snapshot bậc lương tại lúc generate (sau đổi grade không ảnh hưởng lịch sử)
    @Column(name = "grade_level", length = 20)
    private String gradeLevel;

    @Column(name = "base_salary", nullable = false)
    private long baseSalary;

    @Column(name = "allowance", nullable = false)
    private long allowance;

    @Column(name = "overtime_rate", nullable = false)
    private long overtimeRate;

    @Column(name = "overtime_hours", nullable = false)
    private double overtimeHours;

    @Column(name = "overtime_pay", nullable = false)
    private long overtimePay;

    @Column(name = "gross_pay", nullable = false)
    private long grossPay; // base + allowance + overtime (chưa gồm bonus, chưa trừ nghỉ)

    @Column(name = "leave_days", nullable = false)
    @Builder.Default
    private int leaveDays = 0; // ngày nghỉ tính trừ (đã loại off-day)

    @Column(name = "off_days", length = 100)
    private String offDays; // snapshot cấu hình ngày nghỉ lúc generate (vd SUNDAY)

    @Column(name = "bonus", nullable = false)
    @Builder.Default
    private long bonus = 0L;

    @Column(name = "leave_deduction", nullable = false)
    @Builder.Default
    private long leaveDeduction = 0L; // base/26 * leaveDays

    @Column(name = "insurance_deduction", nullable = false)
    private long insuranceDeduction; // 10.5% gross

    @Column(name = "tax_deduction", nullable = false)
    @Builder.Default
    private long taxDeduction = 0L; // kế toán chốt khi approve

    @Column(name = "net_pay", nullable = false)
    private long netPay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.PENDING;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
