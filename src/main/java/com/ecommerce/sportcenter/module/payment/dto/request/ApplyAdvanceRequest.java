package com.ecommerce.sportcenter.module.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ApplyAdvanceRequest {
    @Schema(example = "5", description = "Invoice id để cấn trừ (phải cùng khách, còn nợ >= tiền cọc)")
    @NotNull(message = "Invoice id is required")
    private Integer invoiceId;
}
