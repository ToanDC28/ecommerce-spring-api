package com.ecommerce.sportcenter.module.payroll.dto.request;

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
public class UpdatePayrollRequest {
    @Schema(example = "500000", description = "Thưởng tháng (ghi đè số worker tính)")
    @Min(value = 0, message = "Bonus must be >= 0")
    private Long bonus;

    @Schema(example = "2.5", description = "Giờ tăng ca (ghi đè, tính lại overtimePay)")
    @Min(value = 0, message = "Overtime hours must be >= 0")
    private Double overtimeHours;

    @Schema(example = "300000", description = "Thuế TNCN (ghi đè)")
    @Min(value = 0, message = "Tax must be >= 0")
    private Long taxDeduction;

    @Schema(description = "Note")
    private String note;
}
