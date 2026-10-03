package com.ecommerce.sportcenter.module.material.dto.request;

import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateMaterialRequest {
    @Schema(example = "VT-THEP-CT3-10MM", description = "SKU unique")
    @NotBlank(message = "SKU is required")
    private String sku;

    @Schema(example = "Thép tấm CT3 10mm", description = "Display name")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(example = "1", description = "Category id")
    private Integer categoryId;

    @Schema(example = "Hòa Phát", description = "Brand text, optional")
    private String brand;

    @Schema(example = "KG", description = "Unit: CAI, KG, MET, LIT, BO, HOP, CUON")
    @NotNull(message = "Unit is required")
    private MaterialUnit unit;

    @Schema(example = "25000", description = "Cost price VND")
    @NotNull(message = "Cost price is required")
    @Min(value = 0, message = "Cost price must be >= 0")
    private Long costPrice;

    @Schema(example = "30000", description = "Sell price VND, null = internal only")
    @Min(value = 0, message = "Sell price must be >= 0")
    private Long sellPrice;

    @Schema(example = "10", description = "Min stock alert")
    @Min(value = 0, message = "Min stock must be >= 0")
    @Builder.Default
    private Long minStock = 0L;

    @Schema(example = "Kệ A1", description = "Warehouse location")
    private String location;
}
