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

@Entity
@Table(name = "attendance", indexes = {
        @Index(name = "idx_att_period", columnList = "period"),
        @Index(name = "idx_att_staff", columnList = "staff_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_att_staff_period", columnNames = {"staff_id", "period"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE attendance SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "period", nullable = false, length = 7)
    private String period; // 2026-09

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_id", nullable = false)
    private SalaryGrade salaryGrade; // bậc lương áp dụng tháng đó (phase 1 chưa có StaffProfile)

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(name = "overtime_hours", nullable = false)
    @Builder.Default
    private double overtimeHours = 0.0;

    @Column(name = "leave_days", nullable = false)
    @Builder.Default
    private int leaveDays = 0;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
