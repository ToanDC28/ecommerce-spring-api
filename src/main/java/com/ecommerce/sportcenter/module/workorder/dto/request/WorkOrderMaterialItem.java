package com.ecommerce.sportcenter.module.workorder.dto.request;

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
public class WorkOrderMaterialItem {
    @Schema(example = "1", description = "Material id")
    @NotNull(message = "Material id is required")
    private Integer materialId;

    @Schema(example = "10", description = "Planned quantity")
    @NotNull(message = "Planned quantity is required")
    @Min(value = 1, message = "Planned quantity must be >= 1")
    private Long qtyPlanned;
}
