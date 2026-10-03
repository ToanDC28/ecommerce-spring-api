package com.ecommerce.sportcenter.module.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitResponse {
    private LocalDate from;
    private LocalDate to;
    private long revenue; // WORK + SALES đã thu (PAID/PARTIAL theo issueDate)
    private long workRevenue;
    private long salesRevenue;
    private long materialCost; // WO actual*unitCost + SO sold*qty*costPrice hiện tại (xấp xỉ, xem note)
    private long salaryCost; // payroll PAID trong kỳ giao với range
    private long profit; // revenue - materialCost - salaryCost
}
