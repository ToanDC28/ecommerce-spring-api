package com.ecommerce.sportcenter.module.purchasing.dto.mapper;

import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptItemResponse;
import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptItem;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptNote;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptMapper {

    public GoodsReceiptResponse toResponse(GoodsReceiptNote grn) {
        if (grn == null) {
            return null;
        }
        return GoodsReceiptResponse.builder()
                .id(grn.getId())
                .createdDate(grn.getCreatedDate())
                .code(grn.getCode())
                .purchaseOrderId(grn.getPurchaseOrder() == null ? null : grn.getPurchaseOrder().getId())
                .purchaseOrderCode(grn.getPurchaseOrder() == null ? null : grn.getPurchaseOrder().getCode())
                .warehouseId(grn.getWarehouse() == null ? 0 : grn.getWarehouse().getId())
                .warehouseName(grn.getWarehouse() == null ? null : grn.getWarehouse().getName())
                .supplierId(grn.getSupplier() == null ? null : grn.getSupplier().getId())
                .supplierName(grn.getSupplier() == null ? null : grn.getSupplier().getName())
                .receiptDate(grn.getReceiptDate())
                .type(grn.getType())
                .status(grn.getStatus())
                .items(grn.getItems() == null ? java.util.List.of()
                        : grn.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public GoodsReceiptItemResponse toItemResponse(GoodsReceiptItem item) {
        if (item == null) {
            return null;
        }
        return GoodsReceiptItemResponse.builder()
                .id(item.getId())
                .materialId(item.getMaterial() == null ? 0 : item.getMaterial().getId())
                .materialSku(item.getMaterial() == null ? null : item.getMaterial().getSku())
                .materialName(item.getMaterial() == null ? null : item.getMaterial().getName())
                .qty(item.getQty())
                .unitCost(item.getUnitCost())
                .batchNo(item.getBatchNo())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
