package com.ecommerce.sportcenter.module.sales.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.customer.service.CustomerService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.sales.dto.mapper.SalesOrderMapper;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SalesOrderItemRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderResponse;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderStatus;
import com.ecommerce.sportcenter.module.sales.repository.GoodsIssueNoteRepository;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderRepository;
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
class SalesOrderServiceImplTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private GoodsIssueNoteRepository goodsIssueNoteRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerService customerService;

    private Customer customer() {
        return Customer.builder().id(1).code("KH-001").name("Anh Ba")
                .phone("0901234567").active(true).build();
    }
    @Mock
    private SalesOrderMapper salesOrderMapper;

    @InjectMocks
    private SalesOrderServiceImpl service;

    private Material sellable() {
        return Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).sellPrice(30000L).stockQty(100L).active(true).build();
    }

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success snapshots sellPrice, totals server-side")
        void success() {
            when(materialRepository.findById(1)).thenReturn(Optional.of(sellable()));
            when(customerService.resolveSellPrice(any(), any())).thenAnswer(i ->
                    ((Material) i.getArgument(1)).getSellPrice());
            when(salesOrderRepository.count()).thenReturn(0L);
            when(salesOrderRepository.existsByCode(any())).thenReturn(false);
            when(salesOrderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(salesOrderMapper.toResponse(any())).thenReturn(SalesOrderResponse.builder().code("SO-2026-0001").build());

            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));
            var result = service.create(CreateSalesOrderRequest.builder()
                    .customerId(1).customerName("Anh Ba")
                    .items(List.of(SalesOrderItemRequest.builder().materialId(1).qty(5L).build()))
                    .build(), "admin");

            assertThat(result.getCode()).isEqualTo("SO-2026-0001");
        }

        @Test
        @DisplayName("reject internal-only material (no sellPrice)")
        void internalOnly() {
            var internal = Material.builder().id(2).sku("VT-002").name("Dung dịch nội bộ")
                    .unit(MaterialUnit.LIT).costPrice(10000L).sellPrice(null).active(true).build();
            when(materialRepository.findById(2)).thenReturn(Optional.of(internal));
            when(customerService.resolveSellPrice(any(), any())).thenReturn(null);

            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));
            assertThatThrownBy(() -> service.create(CreateSalesOrderRequest.builder()
                    .customerId(1).customerName("Anh Ba")
                    .items(List.of(SalesOrderItemRequest.builder().materialId(2).qty(5L).build()))
                    .build(), "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("internal-only");
            verify(salesOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject duplicate material lines")
        void duplicateLines() {
            assertThatThrownBy(() -> service.create(CreateSalesOrderRequest.builder()
                    .customerName("Anh Ba")
                    .items(List.of(
                            SalesOrderItemRequest.builder().materialId(1).qty(2L).build(),
                            SalesOrderItemRequest.builder().materialId(1).qty(3L).build()))
                    .build(), "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Duplicate material");
        }

        @Test
        @DisplayName("uses customer special price when set")
        void specialPrice() {
            when(materialRepository.findById(1)).thenReturn(Optional.of(sellable()));
            when(customerService.resolveSellPrice(any(), any())).thenReturn(27000L);
            when(salesOrderRepository.count()).thenReturn(0L);
            when(salesOrderRepository.existsByCode(any())).thenReturn(false);
            var saved = new com.ecommerce.sportcenter.module.sales.entity.SalesOrder[1];
            when(salesOrderRepository.save(any())).thenAnswer(i -> {
                var so = (com.ecommerce.sportcenter.module.sales.entity.SalesOrder) i.getArgument(0);
                saved[0] = so;
                return so;
            });
            when(salesOrderMapper.toResponse(any())).thenReturn(SalesOrderResponse.builder().build());

            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));
            service.create(CreateSalesOrderRequest.builder()
                    .customerId(1).customerName("Anh Ba")
                    .items(List.of(SalesOrderItemRequest.builder().materialId(1).qty(5L).build()))
                    .build(), "admin");

            // 5 * 27000 (giá riêng) thay vì 5 * 30000
            assertThat(saved[0].getGrandTotal()).isEqualTo(5L * 27000L);
        }
    }

    @Nested
    @DisplayName("confirm():")
    class Confirm {

        @Test
        @DisplayName("reject when stock short")
        void shortStock() {
            var low = Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                    .costPrice(25000L).sellPrice(30000L).stockQty(2L).active(true).build();
            var so = SalesOrder.builder().id(1).code("SO-2026-0001").status(SalesOrderStatus.PENDING).build();
            so.getItems().add(com.ecommerce.sportcenter.module.sales.entity.SalesOrderItem.builder()
                    .salesOrder(so).material(low).qty(5L).unitPrice(30000L).lineTotal(150000L).build());
            when(salesOrderRepository.findById(1)).thenReturn(Optional.of(so));

            assertThatThrownBy(() -> service.confirm(1))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }
}
