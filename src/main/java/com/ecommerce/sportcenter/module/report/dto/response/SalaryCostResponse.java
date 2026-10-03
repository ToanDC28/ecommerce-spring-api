package com.ecommerce.sportcenter.module.report.dto.response;

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
public class SalaryCostResponse {
    private String period;
    private long headcount;
    private long totalNet; // sum netPay của payroll PAID trong kỳ
    private long totalGross;
}
