package com.ecommerce.sportcenter.module.inventory.dto.mapper;

import com.ecommerce.sportcenter.module.inventory.dto.response.StockResponse;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockTransactionResponse;
import com.ecommerce.sportcenter.module.inventory.dto.response.WarehouseResponse;
import com.ecommerce.sportcenter.module.inventory.entity.Stock;
import com.ecommerce.sportcenter.module.inventory.entity.StockTransaction;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public WarehouseResponse toResponse(Warehouse w) {
        if (w == null) {
            return null;
        }
        return WarehouseResponse.builder()
                .id(w.getId())
                .code(w.getCode())
                .name(w.getName())
                .address(w.getAddress())
                .active(w.isActive())
                .build();
    }

    public StockResponse toResponse(Stock s) {
        if (s == null) {
            return null;
        }
        var m = s.getMaterial();
        return StockResponse.builder()
                .id(s.getId())
                .warehouseId(s.getWarehouse() == null ? 0 : s.getWarehouse().getId())
                .warehouseName(s.getWarehouse() == null ? null : s.getWarehouse().getName())
                .materialId(m == null ? 0 : m.getId())
                .materialSku(m == null ? null : m.getSku())
                .materialName(m == null ? null : m.getName())
                .unit(m == null ? null : m.getUnit())
                .qtyOnHand(s.getQtyOnHand())
                .qtyReserved(s.getQtyReserved())
                .minStock(m == null ? 0L : m.getMinStock())
                .lowStock(m != null && s.getQtyOnHand() <= m.getMinStock())
                .build();
    }

    public StockTransactionResponse toResponse(StockTransaction t) {
        if (t == null) {
            return null;
        }
        return StockTransactionResponse.builder()
                .id(t.getId())
                .createdDate(t.getCreatedDate())
                .materialId(t.getMaterial() == null ? 0 : t.getMaterial().getId())
                .materialSku(t.getMaterial() == null ? null : t.getMaterial().getSku())
                .materialName(t.getMaterial() == null ? null : t.getMaterial().getName())
                .warehouseName(t.getWarehouse() == null ? null : t.getWarehouse().getName())
                .type(t.getType())
                .refType(t.getRefType())
                .refId(t.getRefId())
                .qtyBefore(t.getQtyBefore())
                .qtyChange(t.getQtyChange())
                .qtyAfter(t.getQtyAfter())
                .createdBy(t.getCreatedBy())
                .build();
    }
}
