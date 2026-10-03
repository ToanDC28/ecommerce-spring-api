package com.ecommerce.sportcenter.module.purchasing.dto.request;

import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGoodsReceiptRequest {
    @Schema(example = "1", description = "Purchase order id, null = mua trực tiếp không qua PO")
    private Integer purchaseOrderId;

    @Schema(example = "1", description = "Warehouse id (required)")
    @NotNull(message = "Warehouse id is required")
    private Integer warehouseId;

    @Schema(example = "2", description = "Supplier id — required khi không có PO")
    private Integer supplierId;

    @Schema(example = "IMPORT_PURCHASE", description = "IMPORT_PURCHASE | IMPORT_RETURN_WORK | IMPORT_RETURN_SALE")
    @Builder.Default
    private GoodsReceiptType type = GoodsReceiptType.IMPORT_PURCHASE;

    @Schema(description = "Receipt lines")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<GoodsReceiptItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptItemRequest {
        @Schema(example = "1", description = "Material id")
        @NotNull(message = "Material id is required")
        private Integer materialId;

        @Schema(example = "100", description = "Received quantity (cân/đo thực tế)")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be >= 1")
        private Long qty;

        @Schema(example = "25000", description = "Unit cost VND")
        @NotNull(message = "Unit cost is required")
        @Min(value = 0, message = "Unit cost must be >= 0")
        private Long unitCost;

        @Schema(example = "LOT-2026-001", description = "Batch number, optional")
        private String batchNo;
    }
}
