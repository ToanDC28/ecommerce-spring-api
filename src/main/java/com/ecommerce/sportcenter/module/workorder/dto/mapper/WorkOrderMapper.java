package com.ecommerce.sportcenter.module.workorder.dto.mapper;

import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderMaterialResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderAttachmentResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderAttachment;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderMaterial;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderMapper {

    public WorkOrderResponse toResponse(WorkOrder wo) {
        return toResponse(wo, java.util.List.of());
    }

    public WorkOrderResponse toResponse(WorkOrder wo, java.util.List<WorkOrderAttachment> attachments) {
        if (wo == null) {
            return null;
        }
        var materials = wo.getMaterials() == null ? java.util.List.<WorkOrderMaterialResponse>of()
                : wo.getMaterials().stream().map(this::toMaterialResponse).toList();
        long plannedCost = materials.stream().mapToLong(WorkOrderMaterialResponse::getPlannedTotal).sum();
        long actualCost = materials.stream().mapToLong(WorkOrderMaterialResponse::getActualTotal).sum();
        var files = attachments == null ? java.util.List.<WorkOrderAttachmentResponse>of()
                : attachments.stream().map(this::toAttachmentResponse).toList();
        return WorkOrderResponse.builder()
                .id(wo.getId())
                .createdDate(wo.getCreatedDate())
                .updatedDate(wo.getUpdatedDate())
                .code(wo.getCode())
                .type(wo.getType())
                .contractNo(wo.getContractNo())
                .customerId(wo.getCustomer() == null ? null : wo.getCustomer().getId())
                .customerName(wo.getCustomerName())
                .customerPhone(wo.getCustomerPhone())
                .machineInfo(wo.getMachineInfo())
                .dueDate(wo.getDueDate())
                .status(wo.getStatus())
                .laborCost(wo.getLaborCost())
                .overheadCost(wo.getOverheadCost())
                .agreedPrice(wo.getAgreedPrice())
                .materialPlannedCost(plannedCost)
                .materialActualCost(actualCost)
                .materials(materials)
                .attachments(files)
                .build();
    }

    public WorkOrderMaterialResponse toMaterialResponse(WorkOrderMaterial m) {
        if (m == null) {
            return null;
        }
        return WorkOrderMaterialResponse.builder()
                .id(m.getId())
                .materialId(m.getMaterial() == null ? 0 : m.getMaterial().getId())
                .materialSku(m.getMaterial() == null ? null : m.getMaterial().getSku())
                .materialName(m.getMaterial() == null ? null : m.getMaterial().getName())
                .qtyPlanned(m.getQtyPlanned())
                .qtyActual(m.getQtyActual())
                .unitCost(m.getUnitCost())
                .unitSellPrice(m.getUnitSellPrice())
                .plannedTotal(m.getQtyPlanned() * m.getUnitCost())
                .actualTotal(m.getQtyActual() * m.getUnitCost())
                .build();
    }

    public WorkOrderAttachmentResponse toAttachmentResponse(WorkOrderAttachment a) {
        if (a == null) {
            return null;
        }
        return WorkOrderAttachmentResponse.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .url(a.getUrl())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .uploadedBy(a.getUploadedBy())
                .build();
    }
}
