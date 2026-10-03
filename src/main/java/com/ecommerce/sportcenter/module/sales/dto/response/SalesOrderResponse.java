package com.ecommerce.sportcenter.module.sales.dto.response;

import com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus;
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
public class SalesOrderResponse {
    private int id;
    private Date createdDate;
    private String code;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private LocalDate orderDate;
    private SalesOrderStatus status;
    private long subTotal;
    private long discount;
    private long grandTotal;
    private String note;
    private List<SalesOrderItemResponse> items;
}
