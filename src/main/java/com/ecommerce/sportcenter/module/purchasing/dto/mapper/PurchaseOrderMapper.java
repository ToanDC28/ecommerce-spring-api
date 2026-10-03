package com.ecommerce.sportcenter.module.purchasing.dto.mapper;

import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderItemResponse;
import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderItem;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder po) {
        if (po == null) {
            return null;
        }
        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .createdDate(po.getCreatedDate())
                .code(po.getCode())
                .supplierId(po.getSupplier() == null ? 0 : po.getSupplier().getId())
                .supplierName(po.getSupplier() == null ? null : po.getSupplier().getName())
                .orderDate(po.getOrderDate())
                .expectedDate(po.getExpectedDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .note(po.getNote())
                .items(po.getItems() == null ? java.util.List.of()
                        : po.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        if (item == null) {
            return null;
        }
        return PurchaseOrderItemResponse.builder()
                .id(item.getId())
                .materialId(item.getMaterial() == null ? 0 : item.getMaterial().getId())
                .materialSku(item.getMaterial() == null ? null : item.getMaterial().getSku())
                .materialName(item.getMaterial() == null ? null : item.getMaterial().getName())
                .qty(item.getQty())
                .unitCost(item.getUnitCost())
                .lineTotal(item.getLineTotal())
                .receivedQty(item.getReceivedQty())
                .build();
    }
}
