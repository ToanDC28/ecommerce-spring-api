package com.ecommerce.sportcenter.module.workorder.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.workorder.dto.mapper.WorkOrderMapper;
import com.ecommerce.sportcenter.module.workorder.dto.request.ConsumeMaterialRequest;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderMaterial;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceImplTest {

    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderMaterialRepository materialLineRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private WorkOrderMapper workOrderMapper;

    @InjectMocks
    private WorkOrderServiceImpl service;

    private WorkOrder wo;
    private Material material;

    @BeforeEach
    void setUp() {
        wo = WorkOrder.builder().id(1).code("WO-2026-0001").type(WorkOrderType.REPAIR)
                .customerName("ABC").status(WorkOrderStatus.CONFIRMED).materials(new ArrayList<>()).build();
        material = Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).sellPrice(30000L).stockQty(100L).active(true).build();
    }

    @Nested
    @DisplayName("consume():")
    class Consume {

        @Test
        @DisplayName("success trừ kho + cộng qtyActual")
        void success() {
            var line = WorkOrderMaterial.builder().id(1).workOrder(wo).material(material)
                    .qtyPlanned(10L).qtyActual(0L).unitCost(25000L).unitSellPrice(30000L).build();
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));
            when(materialLineRepository.findByWorkOrder_Id(1)).thenReturn(new ArrayList<>(List.of(line)));
            when(materialRepository.findById(1)).thenReturn(Optional.of(material));
            when(workOrderRepository.save(wo)).thenReturn(wo);
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));
            var expected = WorkOrderResponse.builder().id(1).code("WO-2026-0001").build();
            when(workOrderMapper.toResponse(any())).thenReturn(expected);

            var request = ConsumeMaterialRequest.builder()
                    .items(List.of(ConsumeMaterialRequest.ConsumeLine.builder().materialId(1).qty(5L).build()))
                    .build();

            assertThat(service.consume(1, request, "admin")).isEqualTo(expected);
            verify(inventoryService).decrease(eq(1), any(), eq(5L), eq(StockRefType.WORK_ORDER), eq("WO-2026-0001"), eq("admin"));
            assertThat(line.getQtyActual()).isEqualTo(5L);
        }

        @Test
        @DisplayName("reject khi WO DRAFT")
        void rejectDraft() {
            wo.setStatus(WorkOrderStatus.DRAFT);
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));

            var request = ConsumeMaterialRequest.builder()
                    .items(List.of(ConsumeMaterialRequest.ConsumeLine.builder().materialId(1).qty(5L).build()))
                    .build();

            assertThatThrownBy(() -> service.consume(1, request, "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("CONFIRMED/IN_PROGRESS");
        }
    }

    @Nested
    @DisplayName("done():")
    class Done {

        @Test
        @DisplayName("success CONFIRMED -> DONE")
        void success() {
            wo.setStatus(WorkOrderStatus.IN_PROGRESS);
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));
            when(workOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(workOrderMapper.toResponse(any())).thenReturn(WorkOrderResponse.builder().id(1).build());

            service.done(1);

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.DONE);
        }
    }
}
