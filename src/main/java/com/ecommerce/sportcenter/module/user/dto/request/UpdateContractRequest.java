package com.ecommerce.sportcenter.module.user.dto.request;

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
public class UpdateContractRequest {
    @Schema(example = "2", description = "Bậc lương mới (trống = giữ nguyên)")
    private Integer salaryGradeId;

    @Schema(example = "9000000", description = "Lương cơ bản thỏa thuận mới (trống = giữ nguyên)")
    @Min(value = 0, message = "Agreed base must be >= 0")
    private Long agreedBaseSalary;
}
