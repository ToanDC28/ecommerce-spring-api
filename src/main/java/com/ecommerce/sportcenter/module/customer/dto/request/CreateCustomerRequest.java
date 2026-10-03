package com.ecommerce.sportcenter.module.customer.dto.request;

import com.ecommerce.sportcenter.module.customer.entity.CustomerType;
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
public class CreateCustomerRequest {
    @Schema(example = "Anh Ba", description = "Customer name")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(example = "0901234567", description = "Phone BẮT BUỘC (chuẩn hóa 0xxxxxxxxx, unique)")
    @NotBlank(message = "Phone is required")
    private String phone;

    @Schema(description = "Address")
    private String address;

    @Schema(example = "HOP_DONG", description = "LE_QUEN | HOP_DONG")
    @Builder.Default
    private CustomerType type = CustomerType.LE_QUEN;
}
