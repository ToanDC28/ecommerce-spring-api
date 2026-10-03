package com.ecommerce.sportcenter.module.payroll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UpsertAttendanceRequest {
    @Schema(example = "2", description = "Staff user id")
    @NotNull(message = "Staff id is required")
    private Integer staffId;

    @Schema(example = "2026-09", description = "Period YYYY-MM")
    @NotNull(message = "Period is required")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Period must be YYYY-MM")
    private String period;

    @Schema(example = "1", description = "Salary grade id áp dụng tháng đó")
    @NotNull(message = "Salary grade is required")
    private Integer salaryGradeId;

    @Schema(example = "26", description = "Working days 0..31")
    @Min(value = 0, message = "Working days must be >= 0")
    @Max(value = 31, message = "Working days must be <= 31")
    @Builder.Default
    private Integer workingDays = 0;

    @Schema(example = "10.5", description = "Overtime hours >= 0")
    @Min(value = 0, message = "Overtime hours must be >= 0")
    @Builder.Default
    private Double overtimeHours = 0.0;

    @Schema(example = "1", description = "Leave days 0..31")
    @Min(value = 0, message = "Leave days must be >= 0")
    @Max(value = 31, message = "Leave days must be <= 31")
    @Builder.Default
    private Integer leaveDays = 0;

    @Schema(description = "Note")
    private String note;
}
