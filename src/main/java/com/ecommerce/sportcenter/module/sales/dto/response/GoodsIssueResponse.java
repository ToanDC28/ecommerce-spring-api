package com.ecommerce.sportcenter.module.sales.dto.response;

import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueStatus;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueType;
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
public class GoodsIssueResponse {
    private int id;
    private Date createdDate;
    private String code;
    private Integer salesOrderId;
    private String salesOrderCode;
    private Integer customerId;
    private String customerName;
    private int warehouseId;
    private String warehouseName;
    private LocalDate issueDate;
    private GoodsIssueType type;
    private GoodsIssueStatus status;
    private List<GoodsIssueItemResponse> items;
}
