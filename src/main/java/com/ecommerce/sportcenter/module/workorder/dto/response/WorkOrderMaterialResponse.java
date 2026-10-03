package com.ecommerce.sportcenter.module.workorder.dto.response;

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
public class WorkOrderMaterialResponse {
    private int id;
    private int materialId;
    private String materialSku;
    private String materialName;
    private long qtyPlanned;
    private long qtyActual;
    private long unitCost;
    private Long unitSellPrice;
    private long plannedTotal;
    private long actualTotal;
}
