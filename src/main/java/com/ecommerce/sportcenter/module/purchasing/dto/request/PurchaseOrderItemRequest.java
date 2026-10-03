package com.ecommerce.sportcenter.module.purchasing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemRequest {
    @Schema(example = "1", description = "Material id")
    @NotNull(message = "Material id is required")
    private Integer materialId;

    @Schema(example = "100", description = "Quantity (KG/CAI/...)")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be >= 1")
    private Long qty;

    @Schema(example = "25000", description = "Unit cost VND")
    @NotNull(message = "Unit cost is required")
    @Min(value = 0, message = "Unit cost must be >= 0")
    private Long unitCost;
}
