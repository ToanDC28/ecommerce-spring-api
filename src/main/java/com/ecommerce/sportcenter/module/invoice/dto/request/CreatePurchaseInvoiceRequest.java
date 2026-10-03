package com.ecommerce.sportcenter.module.invoice.dto.request;

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

/**
 * Internal input for auto-creating a PURCHASE invoice on GRN confirm.
 * Built server-side by the purchasing module — descriptions are snapshots.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseInvoiceRequest {
    @Schema(description = "Supplier id")
    @NotNull(message = "Supplier id is required")
    private Integer supplierId;

    @Schema(description = "Supplier name snapshot")
    @NotNull(message = "Supplier name is required")
    private String supplierName;

    @Schema(example = "GRN-2026-0001", description = "Source receipt code")
    @NotNull(message = "Ref code is required")
    private String refCode;

    @Schema(description = "Lines from receipt items")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<PurchaseInvoiceLine> items;

    @Schema(example = "10", description = "VAT rate: 0, 8, 10")
    @Builder.Default
    private Integer vatRate = 10;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseInvoiceLine {
        private Integer materialId;
        private String description;

        @Min(value = 1, message = "Quantity must be >= 1")
        private long qty;

        @Min(value = 0, message = "Unit cost must be >= 0")
        private long unitCost;
    }
}
