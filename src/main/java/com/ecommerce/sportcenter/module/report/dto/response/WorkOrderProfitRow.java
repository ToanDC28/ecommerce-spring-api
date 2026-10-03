package com.ecommerce.sportcenter.module.report.dto.response;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderProfitRow {
    private int workOrderId;
    private String code;
    private WorkOrderType type;
    private WorkOrderStatus status;
    private String customerName;
    private Long agreedPrice;
    private Long invoicedTotal; // tổng grandTotal các WORK invoice của WO (null nếu chưa xuất)
    private long materialActualCost; // sum(qtyActual * unitCost)
    private long laborCost;
    private long overheadCost;
    private long totalCost;
    private Long revenue; // agreedPrice ?? invoicedTotal
    private Long margin; // revenue - totalCost (null nếu chưa có revenue)
}
