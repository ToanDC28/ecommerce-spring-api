package com.ecommerce.sportcenter.module.purchasing.dto.response;

import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptResponse {
    private int id;
    private Date createdDate;
    private String code;
    private Integer purchaseOrderId;
    private String purchaseOrderCode;
    private int warehouseId;
    private String warehouseName;
    private Integer supplierId;
    private String supplierName;
    private LocalDate receiptDate;
    private GoodsReceiptType type;
    private GoodsReceiptStatus status;
    private List<GoodsReceiptItemResponse> items;
}
