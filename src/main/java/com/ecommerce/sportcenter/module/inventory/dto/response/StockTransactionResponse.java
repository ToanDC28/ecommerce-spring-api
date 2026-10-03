package com.ecommerce.sportcenter.module.inventory.dto.response;

import com.ecommerce.sportcenter.module.inventory.entity.StockMoveType;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {
    private int id;
    private Date createdDate;
    private int materialId;
    private String materialSku;
    private String materialName;
    private String warehouseName;
    private StockMoveType type;
    private StockRefType refType;
    private String refId;
    private long qtyBefore;
    private long qtyChange;
    private long qtyAfter;
    private String createdBy;
}
