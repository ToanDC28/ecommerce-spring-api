package com.ecommerce.sportcenter.module.workorder.dto.request;

import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkOrderRequest {
    @Schema(example = "REPAIR", description = "REPAIR or MANUFACTURE_NEW")
    @NotNull(message = "Type is required")
    private WorkOrderType type;

    @Schema(example = "HD-2026-001", description = "Contract number, optional")
    private String contractNo;

    @Schema(example = "1", description = "Customer id từ Customer master (bắt buộc với WO)")
    @NotNull(message = "Customer id is required")
    private Integer customerId;

    @Schema(example = "Công ty ABC", description = "Customer name snapshot (mặc định theo Customer)")
    private String customerName;

    @Schema(example = "0901234567", description = "Customer phone snapshot (mặc định theo Customer)")
    private String customerPhone;

    @Schema(example = "Máy xúc Komatsu PC200, gãy cần", description = "Machine info")
    private String machineInfo;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(example = "5000000", description = "Labor cost VND")
    @Builder.Default
    private Long laborCost = 0L;

    @Schema(description = "Overhead cost VND")
    @Builder.Default
    private Long overheadCost = 0L;

    @Schema(description = "Agreed price VND")
    private Long agreedPrice;

    @Schema(description = "Planned materials")
    @NotEmpty(message = "Materials are required")
    @Valid
    private List<WorkOrderMaterialItem> items;
}
