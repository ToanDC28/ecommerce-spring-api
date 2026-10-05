package com.ecommerce.sportcenter.module.payroll.dto.mapper;

import com.ecommerce.sportcenter.module.payroll.dto.response.LeaveResponse;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollSettingResponse;
import com.ecommerce.sportcenter.module.payroll.dto.response.SalaryGradeResponse;
import com.ecommerce.sportcenter.module.payroll.entity.Payroll;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollSetting;
import com.ecommerce.sportcenter.module.payroll.entity.SalaryGrade;
import com.ecommerce.sportcenter.module.payroll.entity.StaffLeave;
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

    public LeaveResponse toResponse(StaffLeave l) {
        if (l == null) {
            return null;
        }
        return LeaveResponse.builder()
                .id(l.getId())
                .staffId(l.getStaff() == null ? 0 : l.getStaff().getId())
                .staffUsername(l.getStaff() == null ? null : l.getStaff().getUsername())
                .leaveDate(l.getLeaveDate())
                .note(l.getNote())
                .build();
    }

    public PayrollSettingResponse toResponse(PayrollSetting s) {
        if (s == null) {
            return null;
        }
        return PayrollSettingResponse.builder()
                .offWeekdays(s.getOffWeekdays())
                .standardMonthDays(s.getStandardMonthDays())
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
                .bonus(p.getBonus())
                .leaveDays(p.getLeaveDays())
                .offDays(p.getOffDays())
                .leaveDeduction(p.getLeaveDeduction())
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
