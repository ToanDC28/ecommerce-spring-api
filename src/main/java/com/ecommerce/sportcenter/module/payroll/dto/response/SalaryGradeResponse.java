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
public class SalaryGradeResponse {
    private int id;
    private String level;
    private long baseSalary;
    private long allowance;
    private long overtimeRatePerHour;
    private boolean active;
}
