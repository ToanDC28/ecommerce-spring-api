package com.ecommerce.sportcenter.module.invoice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Internal input for auto-creating a SALES invoice on GIN confirm.
 * Built server-side by the sales module — unit prices are sell-price snapshots.
 * UI tạo Customer trước nếu chưa có; invoice luôn link mã KH (snapshot tên/SĐT tự lấy).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSalesInvoiceRequest {
    @Schema(description = "Sales order id, null = bán lẻ trực tiếp")
    private Integer soId;

    @Schema(description = "Sales order code snapshot")
    private String soCode;

    @Schema(description = "Customer id từ master (BẮT BUỘC)")
    @NotNull(message = "Customer id is required")
    private Integer customerId;

    @Schema(description = "Customer name override hiển thị (mặc định theo Customer)")
    private String customerName;

    @Schema(description = "Customer phone override (mặc định theo Customer)")
    private String customerPhone;

    @Schema(example = "GIN-2026-0001", description = "Source issue code")
    @NotBlank(message = "Ref code is required")
    private String refCode;

    @Schema(description = "Lines from issue items")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<SalesInvoiceLine> items;

    @Schema(example = "0", description = "VAT rate: 0, 8, 10 (bán lẻ thường 0)")
    @Builder.Default
    private Integer vatRate = 0;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesInvoiceLine {
        private Integer materialId;
        private String description;

        @Min(value = 1, message = "Quantity must be >= 1")
        private long qty;

        @Min(value = 0, message = "Unit price must be >= 0")
        private long unitPrice;
    }
}
