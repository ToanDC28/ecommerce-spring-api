package com.ecommerce.sportcenter.module.customer.dto.request;

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
public class SetCustomerPriceRequest {
    @Schema(example = "1", description = "Material id")
    @NotNull(message = "Material id is required")
    private Integer materialId;

    @Schema(example = "27000", description = "Giá bán riêng VND cho khách này")
    @NotNull(message = "Sell price is required")
    @Min(value = 0, message = "Sell price must be >= 0")
    private Long sellPrice;
}
