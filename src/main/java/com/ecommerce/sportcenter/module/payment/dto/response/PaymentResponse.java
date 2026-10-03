package com.ecommerce.sportcenter.module.payment.dto.response;

import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private int id;
    private Date createdDate;
    private String code;
    private int invoiceId;
    private String invoiceCode;
    private long amount;
    private PaymentMethod method;
    private LocalDate paymentDate;
    private PaymentStatus status;
    private String transactionRef;
    private String receivedBy;
    private String note;
}
