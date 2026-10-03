package com.ecommerce.sportcenter.module.invoice.dto.response;

import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private int id;
    private Date createdDate;
    private String code;
    private InvoiceType type;
    private Integer workOrderId;
    private String workOrderCode;
    private Integer supplierId;
    private String supplierName;
    private Integer soId;
    private String soCode;
    private String refCode;
    private String customerName;
    private Integer customerId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private long subTotal;
    private long discountAmount;
    private int vatRate;
    private long vatAmount;
    private long grandTotal;
    private long paidAmount;
    private InvoiceStatus status;
    private List<InvoiceItemResponse> items;
}
