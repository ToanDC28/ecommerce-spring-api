package com.ecommerce.sportcenter.module.payment.dto.response;

import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private List<DayLine> lines;
    private long totalCash;
    private long totalBank;
    private long grandTotal;
    private long count;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayLine {
        private LocalDate date;
        private PaymentMethod method;
        private long totalAmount;
        private long count;
    }
}
