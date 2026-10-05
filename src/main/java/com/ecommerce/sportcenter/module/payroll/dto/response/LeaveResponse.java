package com.ecommerce.sportcenter.module.payroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponse {
    private int id;
    private int staffId;
    private String staffUsername;
    private LocalDate leaveDate;
    private String note;
}
