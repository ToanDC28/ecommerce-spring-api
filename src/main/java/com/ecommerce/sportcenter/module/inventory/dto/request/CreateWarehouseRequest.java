package com.ecommerce.sportcenter.module.inventory.dto.request;

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
public class CreateWarehouseRequest {
    @Schema(example = "WH-XUONG-01", description = "Warehouse code UNIQUE")
    @NotBlank(message = "Code is required")
    private String code;

    @Schema(example = "Kho chính", description = "Warehouse name")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Address")
    private String address;
}
