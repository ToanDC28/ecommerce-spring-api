package com.ecommerce.sportcenter.module.purchasing.dto.response;

import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderStatus;
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
public class PurchaseOrderResponse {
    private int id;
    private Date createdDate;
    private String code;
    private int supplierId;
    private String supplierName;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private PurchaseOrderStatus status;
    private long totalAmount;
    private String note;
    private List<PurchaseOrderItemResponse> items;
}
