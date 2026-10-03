package com.ecommerce.sportcenter.module.supplier.dto.request;

import com.ecommerce.sportcenter.module.supplier.entity.PaymentTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateSupplierRequest {
    @Schema(example = "ACME Parts", description = "Display name")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(example = "0312345678", description = "Tax code")
    private String taxCode;
    @Schema(example = "0901234567", description = "Phone")
    private String phone;
    @Schema(example = "contact@acme.local", description = "Email")
    private String email;
    @Schema(example = "HCMC", description = "Address")
    private String address;
    @Schema(example = "NET_30", description = "Payment term")
    private PaymentTerm paymentTerm;
}
