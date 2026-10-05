package com.ecommerce.sportcenter.module.payroll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordLeaveRequest {
    @Schema(example = "2", description = "Staff user id")
    @NotNull(message = "Staff id is required")
    private Integer staffId;

    @Schema(example = "2026-09-15", description = "Ngày nghỉ")
    @NotNull(message = "Leave date is required")
    private LocalDate leaveDate;

    @Schema(description = "Note (ốm, việc gia đình...)")
    private String note;
}
