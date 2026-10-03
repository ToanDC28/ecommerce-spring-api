package com.ecommerce.sportcenter.module.inventory.dto.response;

import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
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
public class StockResponse {
    private int id;
    private int warehouseId;
    private String warehouseName;
    private int materialId;
    private String materialSku;
    private String materialName;
    private MaterialUnit unit;
    private long qtyOnHand;
    private long qtyReserved;
    private long minStock;
    private boolean lowStock;
}
