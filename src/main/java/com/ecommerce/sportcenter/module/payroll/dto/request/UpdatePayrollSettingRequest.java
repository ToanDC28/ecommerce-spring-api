package com.ecommerce.sportcenter.module.payroll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdatePayrollSettingRequest {
    @Schema(example = "SATURDAY,SUNDAY", description = "Ngày nghỉ hợp lệ trong tuần (CSV DayOfWeek)")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$",
            message = "offWeekdays must be CSV DayOfWeek")
    private String offWeekdays;

    @Schema(example = "26", description = "Công chuẩn tháng")
    @Min(value = 1, message = "Standard days must be >= 1")
    @Max(value = 31, message = "Standard days must be <= 31")
    private Integer standardMonthDays;
}
