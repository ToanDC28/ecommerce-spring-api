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
public class CreatePaymentRequest {
    @Schema(example = "500000", description = "Amount VND (> 0, không vượt còn nợ)")
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be > 0")
    private Long amount;

    @Schema(example = "CASH", description = "CASH | BANK_TRANSFER")
    @NotNull(message = "Method is required")
    private PaymentMethod method;

    @Schema(example = "MBVCB123456", description = "Mã CK ngân hàng — bắt buộc với BANK_TRANSFER, trống với CASH (chống thu trùng)")
    private String transactionRef;

    @Schema(example = "Khách trả 1 phần, còn nợ 1tr", description = "Note")
    private String note;
}
