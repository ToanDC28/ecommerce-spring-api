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
public class DebtRow {
    private Integer customerId; // null = khách vãng lai snapshot / NCC
    private String name; // supplier/customer name
    private String phone;
    private long invoiceCount;
    private long totalOwed; // sum(grandTotal - paidAmount)
}
