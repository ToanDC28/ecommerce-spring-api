package com.ecommerce.sportcenter.module.sales.dto.request;

import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueType;
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
public class CreateGoodsIssueRequest {
    @Schema(example = "1", description = "Sales order id, null = bán lẻ trực tiếp tại quầy")
    private Integer salesOrderId;

    @Schema(example = "1", description = "Warehouse id (required)")
    @NotNull(message = "Warehouse id is required")
    private Integer warehouseId;

    @Schema(example = "Anh Ba", description = "Customer name override (mặc định theo SO/Customer)")
    private String customerName;

    @Schema(example = "2", description = "Customer id từ master — BẮT BUỘC khi bán trực tiếp (UI tạo khách trước)")
    private Integer customerId;

    @Schema(example = "EXPORT_SALE", description = "EXPORT_SALE | EXPORT_RETURN")
    @Builder.Default
    private GoodsIssueType type = GoodsIssueType.EXPORT_SALE;

    @Schema(description = "Issue lines")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<GoodsIssueItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsIssueItemRequest {
        @Schema(example = "1", description = "Material id")
        @NotNull(message = "Material id is required")
        private Integer materialId;

        @Schema(example = "5", description = "Quantity")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be >= 1")
        private Long qty;
    }
}
