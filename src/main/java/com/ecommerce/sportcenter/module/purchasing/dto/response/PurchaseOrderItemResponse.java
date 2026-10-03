package com.ecommerce.sportcenter.module.purchasing.dto.response;

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
public class PurchaseOrderItemResponse {
    private int id;
    private int materialId;
    private String materialSku;
    private String materialName;
    private long qty;
    private long unitCost;
    private long lineTotal;
    private long receivedQty;
}
