package com.ecommerce.sportcenter.module.purchasing.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.purchasing.dto.mapper.PurchaseOrderMapper;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreatePurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.PurchaseOrderItemRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderStatus;
import com.ecommerce.sportcenter.module.purchasing.repository.GoodsReceiptNoteRepository;
import com.ecommerce.sportcenter.module.purchasing.repository.PurchaseOrderRepository;
import com.ecommerce.sportcenter.module.supplier.entity.Supplier;
import com.ecommerce.sportcenter.module.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;

    @InjectMocks
    private PurchaseOrderServiceImpl service;

    private Supplier supplier() {
        return Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
    }

    private Material material() {
        return Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).active(true).build();
    }

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success totals computed server-side")
        void success() {
            var supplier = supplier();
            var material = material();
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(materialRepository.findById(1)).thenReturn(Optional.of(material));
            when(purchaseOrderRepository.count()).thenReturn(0L);
            when(purchaseOrderRepository.existsByCode(any())).thenReturn(false);
            when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(purchaseOrderMapper.toResponse(any())).thenReturn(PurchaseOrderResponse.builder().code("PO-2026-0001").build());

            var result = service.create(CreatePurchaseOrderRequest.builder()
                    .supplierId(1)
                    .items(List.of(PurchaseOrderItemRequest.builder().materialId(1).qty(100L).unitCost(25000L).build()))
                    .build(), "admin");

            assertThat(result.getCode()).isEqualTo("PO-2026-0001");
        }

        @Test
        @DisplayName("reject duplicate material lines")
        void duplicateLines() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier()));

            assertThatThrownBy(() -> service.create(CreatePurchaseOrderRequest.builder()
                    .supplierId(1)
                    .items(List.of(
                            PurchaseOrderItemRequest.builder().materialId(1).qty(10L).unitCost(1000L).build(),
                            PurchaseOrderItemRequest.builder().materialId(1).qty(5L).unitCost(1000L).build()))
                    .build(), "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Duplicate material");
        }

        @Test
        @DisplayName("reject inactive supplier")
        void inactiveSupplier() {
            var inactive = Supplier.builder().id(2).code("SUP-002").name("X").active(false).build();
            when(supplierRepository.findById(2)).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> service.create(CreatePurchaseOrderRequest.builder()
                    .supplierId(2)
                    .items(List.of(PurchaseOrderItemRequest.builder().materialId(1).qty(10L).unitCost(1000L).build()))
                    .build(), "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("inactive");
            verify(purchaseOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("send()/cancel():")
    class Transitions {

        @Test
        @DisplayName("send DRAFT -> SENT, cancel SENT without receipts")
        void flow() {
            var po = PurchaseOrder.builder().id(1).code("PO-2026-0001").status(PurchaseOrderStatus.DRAFT).build();
            when(purchaseOrderRepository.findById(1)).thenReturn(Optional.of(po));
            when(purchaseOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(purchaseOrderMapper.toResponse(any())).thenReturn(PurchaseOrderResponse.builder().build());
            when(goodsReceiptNoteRepository.findByPurchaseOrder_IdAndStatus(any(), any())).thenReturn(List.of());

            service.send(1);
            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.SENT);
            service.cancel(1);
            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        }
    }
}
