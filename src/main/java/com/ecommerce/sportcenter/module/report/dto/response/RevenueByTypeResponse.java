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
public class RevenueByTypeResponse {
    private long workTotal;
    private long workCount;
    private long salesTotal;
    private long salesCount;
    private long grandTotal;
}
