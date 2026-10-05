package com.ecommerce.sportcenter.module.payroll.dto.response;

import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {
    private int id;
    private Date createdDate;
    private int staffId;
    private String staffUsername;
    private String period;
    private String gradeLevel;
    private long baseSalary;
    private long allowance;
    private long overtimeRate;
    private double overtimeHours;
    private long overtimePay;
    private long grossPay;
    private long bonus;
    private int leaveDays;
    private String offDays;
    private long leaveDeduction;
    private long insuranceDeduction;
    private long taxDeduction;
    private long netPay;
    private PayrollStatus status;
    private String approvedBy;
    private LocalDate paidAt;
    private String note;
}
