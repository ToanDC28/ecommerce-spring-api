package com.ecommerce.sportcenter.module.purchasing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseOrderRequest {
    @Schema(example = "1", description = "Supplier id")
    @NotNull(message = "Supplier id is required")
    private Integer supplierId;

    @Schema(description = "Expected date")
    private LocalDate expectedDate;

    @Schema(example = "Mua thép tháng 10", description = "Note")
    private String note;

    @Schema(description = "Order lines (1 line per material)")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
