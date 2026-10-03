package com.ecommerce.sportcenter.module.supplier.dto.response;

import com.ecommerce.sportcenter.module.supplier.entity.PaymentTerm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierResponse {
    private int id;
    private Date createdDate;
    private Date updatedDate;
    private String code;
    private String name;
    private String taxCode;
    private String phone;
    private String email;
    private String address;
    private PaymentTerm paymentTerm;
    private boolean active;
    private long currentDebt;
}
