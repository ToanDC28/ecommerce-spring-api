package com.ecommerce.sportcenter.module.sales.dto.request;

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
public class SalesOrderItemRequest {
    @Schema(example = "1", description = "Material id (phải có sellPrice, đang active)")
    @NotNull(message = "Material id is required")
    private Integer materialId;

    @Schema(example = "5", description = "Quantity")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be >= 1")
    private Long qty;

    @Schema(example = "0", description = "Item discount VND")
    @Min(value = 0, message = "Discount must be >= 0")
    @Builder.Default
    private Long discount = 0L;
}
