package com.ecommerce.sportcenter.module.payroll.dto.request;

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
public class CreateSalaryGradeRequest {
    @Schema(example = "L3", description = "Grade level UNIQUE (L1..L5)")
    @NotBlank(message = "Level is required")
    private String level;

    @Schema(example = "8000000", description = "Base salary VND")
    @NotNull(message = "Base salary is required")
    @Min(value = 0, message = "Base salary must be >= 0")
    private Long baseSalary;

    @Schema(example = "1000000", description = "Allowance VND")
    @Min(value = 0, message = "Allowance must be >= 0")
    @Builder.Default
    private Long allowance = 0L;

    @Schema(example = "50000", description = "Overtime rate per hour VND")
    @NotNull(message = "Overtime rate is required")
    @Min(value = 0, message = "Overtime rate must be >= 0")
    private Long overtimeRatePerHour;
}
