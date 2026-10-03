package com.ecommerce.sportcenter.module.payroll.dto.mapper;

import com.ecommerce.sportcenter.module.payroll.dto.response.AttendanceResponse;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.dto.response.SalaryGradeResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Attendance;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import org.springframework.stereotype.Component;

@Component
public class PayrollMapper {

    public SalaryGradeResponse toResponse(SalaryGrade g) {
        if (g == null) {
            return null;
        }
        return SalaryGradeResponse.builder()
                .id(g.getId())
                .level(g.getLevel())
                .baseSalary(g.getBaseSalary())
                .allowance(g.getAllowance())
                .overtimeRatePerHour(g.getOvertimeRatePerHour())
                .active(g.isActive())
                .build();
    }

    public AttendanceResponse toResponse(Attendance a) {
        if (a == null) {
            return null;
        }
        return AttendanceResponse.builder()
                .id(a.getId())
                .staffId(a.getStaff() == null ? 0 : a.getStaff().getId())
                .staffUsername(a.getStaff() == null ? null : a.getStaff().getUsername())
                .period(a.getPeriod())
                .salaryGradeId(a.getSalaryGrade() == null ? 0 : a.getSalaryGrade().getId())
                .gradeLevel(a.getSalaryGrade() == null ? null : a.getSalaryGrade().getLevel())
                .workingDays(a.getWorkingDays())
                .overtimeHours(a.getOvertimeHours())
                .leaveDays(a.getLeaveDays())
                .note(a.getNote())
                .build();
    }

    public PayrollResponse toResponse(Payroll p) {
        if (p == null) {
            return null;
        }
        return PayrollResponse.builder()
                .id(p.getId())
                .createdDate(p.getCreatedDate())
                .staffId(p.getStaff() == null ? 0 : p.getStaff().getId())
                .staffUsername(p.getStaff() == null ? null : p.getStaff().getUsername())
                .period(p.getPeriod())
                .gradeLevel(p.getGradeLevel())
                .baseSalary(p.getBaseSalary())
                .allowance(p.getAllowance())
                .overtimeRate(p.getOvertimeRate())
                .overtimeHours(p.getOvertimeHours())
                .overtimePay(p.getOvertimePay())
                .grossPay(p.getGrossPay())
                .insuranceDeduction(p.getInsuranceDeduction())
                .taxDeduction(p.getTaxDeduction())
                .netPay(p.getNetPay())
                .status(p.getStatus())
                .approvedBy(p.getApprovedBy())
                .paidAt(p.getPaidAt())
                .note(p.getNote())
                .build();
    }
}
