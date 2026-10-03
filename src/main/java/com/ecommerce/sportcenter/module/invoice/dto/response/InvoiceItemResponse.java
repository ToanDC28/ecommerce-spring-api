package com.ecommerce.sportcenter.module.invoice.dto.response;

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
public class InvoiceItemResponse {
    private int id;
    private Integer materialId;
    private String description;
    private long qty;
    private long unitPrice;
    private long discount;
    private long lineTotal;
}
