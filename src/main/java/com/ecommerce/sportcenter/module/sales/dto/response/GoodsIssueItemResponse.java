package com.ecommerce.sportcenter.module.sales.dto.response;

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
public class GoodsIssueItemResponse {
    private int id;
    private int materialId;
    private String materialSku;
    private String materialName;
    private long qty;
    private long unitPrice;
    private long lineTotal;
}
