package com.ecommerce.sportcenter.module.payroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private int id;
    private int staffId;
    private String staffUsername;
    private String period;
    private int salaryGradeId;
    private String gradeLevel;
    private int workingDays;
    private double overtimeHours;
    private int leaveDays;
    private String note;
}
