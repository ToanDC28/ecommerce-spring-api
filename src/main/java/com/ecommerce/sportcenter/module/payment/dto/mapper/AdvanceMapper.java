package com.ecommerce.sportcenter.module.payment.dto.mapper;

import com.ecommerce.sportcenter.module.payment.dto.response.AdvanceResponse;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceDeposit;
import org.springframework.stereotype.Component;

@Component
public class AdvanceMapper {

    public AdvanceResponse toResponse(AdvanceDeposit a) {
        if (a == null) {
            return null;
        }
        return AdvanceResponse.builder()
                .id(a.getId())
                .createdDate(a.getCreatedDate())
                .code(a.getCode())
                .customerId(a.getCustomer() == null ? 0 : a.getCustomer().getId())
                .customerName(a.getCustomer() == null ? null : a.getCustomer().getName())
                .workOrderId(a.getWorkOrderId())
                .workOrderCode(a.getWorkOrderCode())
                .salesOrderId(a.getSalesOrderId())
                .salesOrderCode(a.getSalesOrderCode())
                .amount(a.getAmount())
                .method(a.getMethod())
                .status(a.getStatus())
                .appliedInvoiceId(a.getAppliedInvoiceId())
                .receivedBy(a.getReceivedBy())
                .note(a.getNote())
                .build();
    }
}
