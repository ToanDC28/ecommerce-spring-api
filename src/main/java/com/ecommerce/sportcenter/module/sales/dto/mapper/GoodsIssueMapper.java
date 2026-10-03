package com.ecommerce.sportcenter.module.sales.dto.mapper;

import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueItemResponse;
import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueResponse;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueItem;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueNote;
import org.springframework.stereotype.Component;

@Component
public class GoodsIssueMapper {

    public GoodsIssueResponse toResponse(GoodsIssueNote gin) {
        if (gin == null) {
            return null;
        }
        return GoodsIssueResponse.builder()
                .id(gin.getId())
                .createdDate(gin.getCreatedDate())
                .code(gin.getCode())
                .salesOrderId(gin.getSalesOrder() == null ? null : gin.getSalesOrder().getId())
                .salesOrderCode(gin.getSalesOrder() == null ? null : gin.getSalesOrder().getCode())
                .customerId(gin.getCustomer() == null
                        ? (gin.getSalesOrder() == null || gin.getSalesOrder().getCustomer() == null ? null
                                : gin.getSalesOrder().getCustomer().getId())
                        : gin.getCustomer().getId())
                .customerName(gin.getSalesOrder() != null ? gin.getSalesOrder().getCustomerName() : gin.getCustomerName())
                .warehouseId(gin.getWarehouse() == null ? 0 : gin.getWarehouse().getId())
                .warehouseName(gin.getWarehouse() == null ? null : gin.getWarehouse().getName())
                .issueDate(gin.getIssueDate())
                .type(gin.getType())
                .status(gin.getStatus())
                .items(gin.getItems() == null ? java.util.List.of()
                        : gin.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public GoodsIssueItemResponse toItemResponse(GoodsIssueItem item) {
        if (item == null) {
            return null;
        }
        return GoodsIssueItemResponse.builder()
                .id(item.getId())
                .materialId(item.getMaterial() == null ? 0 : item.getMaterial().getId())
                .materialSku(item.getMaterial() == null ? null : item.getMaterial().getSku())
                .materialName(item.getMaterial() == null ? null : item.getMaterial().getName())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
