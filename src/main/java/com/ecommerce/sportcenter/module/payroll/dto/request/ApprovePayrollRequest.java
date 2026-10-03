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
public class ApprovePayrollRequest {
    @Schema(example = "500000", description = "Thuế TNCN chốt khi duyệt (mặc định 0)")
    @Min(value = 0, message = "Tax must be >= 0")
    private Long taxDeduction;

    @Schema(description = "Note")
    private String note;
}
