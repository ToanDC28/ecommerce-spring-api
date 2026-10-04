package com.ecommerce.sportcenter.module.payment.dto.response;

import com.ecommerce.sportcenter.module.payment.entity.AdvanceStatus;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceResponse {
    private int id;
    private Date createdDate;
    private String code;
    private int customerId;
    private String customerName;
    private Integer workOrderId;
    private String workOrderCode;
    private Integer salesOrderId;
    private String salesOrderCode;
    private long amount;
    private PaymentMethod method;
    private AdvanceStatus status;
    private Integer appliedInvoiceId;
    private String receivedBy;
    private String note;
}
