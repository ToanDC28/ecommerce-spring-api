package com.ecommerce.sportcenter.module.payment.dto.mapper;

import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment p) {
        if (p == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(p.getId())
                .createdDate(p.getCreatedDate())
                .code(p.getCode())
                .invoiceId(p.getInvoice() == null ? 0 : p.getInvoice().getId())
                .invoiceCode(p.getInvoice() == null ? null : p.getInvoice().getCode())
                .amount(p.getAmount())
                .method(p.getMethod())
                .paymentDate(p.getPaymentDate())
                .status(p.getStatus())
                .transactionRef(p.getTransactionRef())
                .receivedBy(p.getReceivedBy())
                .note(p.getNote())
                .build();
    }
}
