package com.ecommerce.sportcenter.module.payroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSettingResponse {
    private String offWeekdays; // CSV DayOfWeek: SATURDAY,SUNDAY
    private int standardMonthDays; // công chuẩn, mặc định 26
}
