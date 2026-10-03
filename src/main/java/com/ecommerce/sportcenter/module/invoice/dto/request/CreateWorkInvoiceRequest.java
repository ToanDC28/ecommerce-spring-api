package com.ecommerce.sportcenter.module.invoice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
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
public class CreateWorkInvoiceRequest {
    @Schema(example = "1", description = "WorkOrder DONE id")
    @NotNull(message = "Work order id is required")
    private Integer workOrderId;

    @Schema(example = "10", description = "VAT rate: 0, 8, 10")
    @Builder.Default
    private Integer vatRate = 10;

    @Schema(example = "0", description = "Discount amount VND")
    @Builder.Default
    private Long discountAmount = 0L;

    @Schema(description = "Due date offset days, optional")
    @Min(value = 0, message = "Due days must be >= 0")
    @Max(value = 365, message = "Due days must be <= 365")
    private Integer dueDays;
}
