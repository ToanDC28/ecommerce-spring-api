package com.ecommerce.sportcenter.module.workorder.dto.response;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
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
public class WorkOrderResponse {
    private int id;
    private Date createdDate;
    private Date updatedDate;
    private String code;
    private WorkOrderType type;
    private String contractNo;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String machineInfo;
    private LocalDate dueDate;
    private WorkOrderStatus status;
    private long laborCost;
    private long overheadCost;
    private Long agreedPrice;
    private long materialPlannedCost;
    private long materialActualCost;
    private List<WorkOrderMaterialResponse> materials;
    private List<WorkOrderAttachmentResponse> attachments;
}
