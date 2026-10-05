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

/**
 * Cấu hình lương singleton (id luôn = 1): ngày nghỉ hợp lệ trong tuần
 * (vd SUNDAY, hoặc SATURDAY,SUNDAY) + công chuẩn tháng.
 * Ngày nghỉ rơi vào off-day không bị trừ lương.
 */
@Entity
@Table(name = "payroll_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE payroll_setting SET deleted_at = CURRENT_TIMESTAMP WHERE Id = ?")
@SQLRestriction("deleted_at IS NULL")
public class PayrollSetting extends BaseEntity {

    @Column(name = "off_weekdays", nullable = false, length = 100)
    @Builder.Default
    private String offWeekdays = "SUNDAY"; // CSV DayOfWeek: SATURDAY,SUNDAY

    @Column(name = "standard_month_days", nullable = false)
    @Builder.Default
    private int standardMonthDays = 26;
}
