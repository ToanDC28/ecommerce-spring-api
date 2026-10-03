package com.ecommerce.sportcenter.module.payroll.entity;

import com.ecommerce.sportcenter.module.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "salary_grade", indexes = {
        @Index(name = "idx_grade_level", columnList = "level")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_grade_level", columnNames = {"level"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE salary_grade SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class SalaryGrade extends BaseEntity {

    @Column(name = "level", nullable = false, length = 20)
    private String level; // L1..L5 (thợ chính/phụ, kho, kế toán...)

    @Column(name = "base_salary", nullable = false)
    private long baseSalary;

    @Column(name = "allowance", nullable = false)
    @Builder.Default
    private long allowance = 0L;

    @Column(name = "overtime_rate", nullable = false)
    private long overtimeRatePerHour;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
