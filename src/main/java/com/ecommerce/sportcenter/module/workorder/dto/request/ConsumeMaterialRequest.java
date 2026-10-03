package com.ecommerce.sportcenter.module.workorder.dto.request;

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
public class ConsumeMaterialRequest {
    @Schema(description = "Warehouse id, optional (null = default, only Material stock)")
    private Integer warehouseId;

    @Schema(description = "Actual consume lines")
    @NotEmpty(message = "Items are required")
    @Valid
    private List<ConsumeLine> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsumeLine {
        @NotNull(message = "Material id is required")
        private Integer materialId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be >= 1")
        private Long qty;
    }
}
