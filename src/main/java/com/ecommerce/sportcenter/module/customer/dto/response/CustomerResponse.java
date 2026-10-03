package com.ecommerce.sportcenter.module.customer.dto.response;

import com.ecommerce.sportcenter.module.customer.entity.CustomerType;
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
public class CustomerResponse {
    private int id;
    private Date createdDate;
    private String code;
    private String name;
    private String phone;
    private String address;
    private CustomerType type;
    private boolean active;
    private long openInvoiceCount;
    private long totalOwed; // tổng nợ = sum(grandTotal - paidAmount) các invoice chưa trả
}
