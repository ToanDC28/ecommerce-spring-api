package com.ecommerce.sportcenter.module.sales.dto.mapper;

import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderItemResponse;
import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderResponse;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderItem;
import org.springframework.stereotype.Component;

@Component
public class SalesOrderMapper {

    public SalesOrderResponse toResponse(SalesOrder so) {
        if (so == null) {
            return null;
        }
        return SalesOrderResponse.builder()
                .id(so.getId())
                .createdDate(so.getCreatedDate())
                .code(so.getCode())
                .customerId(so.getCustomer() == null ? null : so.getCustomer().getId())
                .customerName(so.getCustomerName())
                .customerPhone(so.getCustomerPhone())
                .orderDate(so.getOrderDate())
                .status(so.getStatus())
                .subTotal(so.getSubTotal())
                .discount(so.getDiscount())
                .grandTotal(so.getGrandTotal())
                .note(so.getNote())
                .items(so.getItems() == null ? java.util.List.of()
                        : so.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public SalesOrderItemResponse toItemResponse(SalesOrderItem item) {
        if (item == null) {
            return null;
        }
        return SalesOrderItemResponse.builder()
                .id(item.getId())
                .materialId(item.getMaterial() == null ? 0 : item.getMaterial().getId())
                .materialSku(item.getMaterial() == null ? null : item.getMaterial().getSku())
                .materialName(item.getMaterial() == null ? null : item.getMaterial().getName())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .lineTotal(item.getLineTotal())
                .issuedQty(item.getIssuedQty())
                .returnedQty(item.getReturnedQty())
                .build();
    }
}
