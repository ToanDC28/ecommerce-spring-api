package com.ecommerce.sportcenter.module.sales.dto.request;

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
public class CreateSalesOrderRequest {
    @Schema(example = "1", description = "Customer id từ master (BẮT BUỘC — UI tạo khách trước nếu chưa có)")
    @NotNull(message = "Customer id is required")
    private Integer customerId;

    @Schema(example = "Anh Ba", description = "Customer name override hiển thị (mặc định theo Customer)")
    private String customerName;

    @Schema(example = "0901234567", description = "Customer phone override (mặc định theo Customer)")
    private String customerPhone;

    @Schema(example = "0", description = "Order discount VND")
    @Min(value = 0, message = "Discount must be >= 0")
    @Builder.Default
    private Long discount = 0L;

    @Schema(example = "Giao trong ngày")
    private String note;

    @Schema(description = "Order lines (1 line per material)")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<SalesOrderItemRequest> items;
}
