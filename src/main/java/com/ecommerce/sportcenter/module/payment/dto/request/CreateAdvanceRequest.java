package com.ecommerce.sportcenter.module.payment.dto.request;

import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
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
public class CreateAdvanceRequest {
    @Schema(example = "1", description = "Work order id cọc cho — bắt buộc 1 trong 2 (WO hoặc SO)")
    private Integer workOrderId;

    @Schema(example = "2", description = "Sales order id cọc cho — bắt buộc 1 trong 2 (WO hoặc SO)")
    private Integer salesOrderId;

    @Schema(example = "2000000", description = "Amount VND > 0")
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be > 0")
    private Long amount;

    @Schema(example = "CASH", description = "CASH | BANK_TRANSFER")
    @NotNull(message = "Method is required")
    private PaymentMethod method;

    @Schema(description = "Mã CK (bắt buộc với BANK_TRANSFER)")
    private String transactionRef;

    @Schema(description = "Note (vd cọc 30% hợp đồng HD-001)")
    private String note;
}
