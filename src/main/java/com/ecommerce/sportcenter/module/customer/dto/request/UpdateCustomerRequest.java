package com.ecommerce.sportcenter.module.customer.dto.request;

import com.ecommerce.sportcenter.module.customer.entity.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class UpdateCustomerRequest {
    @Schema(description = "Customer name")
    private String name;

    @Schema(description = "Phone (chuẩn hóa, unique khi có)")
    private String phone;

    @Schema(description = "Address")
    private String address;

    @Schema(description = "Customer type")
    private CustomerType type;

    @Schema(description = "Active flag")
    private Boolean active;
}
