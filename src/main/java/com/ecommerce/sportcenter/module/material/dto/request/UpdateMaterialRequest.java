package com.ecommerce.sportcenter.module.material.dto.request;

import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
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
public class UpdateMaterialRequest {
    @Schema(example = "Thép tấm CT3 10mm", description = "Display name")
    private String name;

    @Schema(description = "Category id")
    private Integer categoryId;

    @Schema(description = "Brand text")
    private String brand;

    @Schema(description = "Unit")
    private MaterialUnit unit;

    @Schema(description = "Cost price VND")
    @Min(value = 0, message = "Cost price must be >= 0")
    private Long costPrice;

    @Schema(description = "Sell price VND")
    @Min(value = 0, message = "Sell price must be >= 0")
    private Long sellPrice;

    @Schema(description = "Min stock alert")
    @Min(value = 0, message = "Min stock must be >= 0")
    private Long minStock;

    @Schema(description = "Warehouse location")
    private String location;

    @Schema(description = "Active flag")
    private Boolean active;

    // Thông số kỹ thuật kim loại — tất cả optional, chỉ set khi có giá trị
    @Schema(description = "Mác vật liệu")
    private String materialGrade;

    @Schema(description = "Tiêu chuẩn")
    private String standard;

    @Schema(description = "Quy cách chính")
    private String spec;

    @Schema(description = "Dày (mm)")
    private Double thicknessMm;

    @Schema(description = "Rộng (mm)")
    private Double widthMm;

    @Schema(description = "Dài (mm)")
    private Double lengthMm;

    @Schema(description = "Đường kính (mm)")
    private Double diameterMm;

    @Schema(description = "Cấp bền")
    private String strengthGrade;

    @Schema(description = "Chi tiết kỹ thuật còn lại")
    private String detail;
}
