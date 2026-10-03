package com.ecommerce.sportcenter.module.sales.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.sales.dto.mapper.GoodsIssueMapper;
import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueResponse;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueItem;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueNote;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueStatus;
import com.ecommerce.sportcenter.module.sales.entity.GoodsIssueType;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrder;
import com.ecommerce.sportcenter.module.sales.entity.SalesOrderItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsIssueServiceImplTest {

    @Mock
    private GoodsIssueNoteRepository goodsIssueNoteRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private GoodsIssueMapper goodsIssueMapper;

    @InjectMocks
    private GoodsIssueServiceImpl service;

    private Material material() {
        return Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).sellPrice(30000L).stockQty(100L).active(true).build();
    }

    private Customer customer() {
        return Customer.builder().id(1).code("KH-001").name("Anh Ba")
                .phone("0901234567").active(true).build();
    }

    @Nested
    @DisplayName("confirm() EXPORT_SALE:")
    class ConfirmSale {

        @Test
        @DisplayName("success Stock-, SO -> COMPLETED, auto SALES invoice")
        void success() {
            var so = SalesOrder.builder().id(1).code("SO-2026-0001").customer(customer()).customerName("Anh Ba")
                    .status(SalesOrderStatus.DELIVERING).items(new ArrayList<>()).build();
            var soLine = SalesOrderItem.builder().salesOrder(so).material(material())
                    .qty(10L).unitPrice(30000L).lineTotal(300000L).issuedQty(0L).returnedQty(0L).build();
            so.getItems().add(soLine);
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var gin = GoodsIssueNote.builder().id(1).code("GIN-2026-0001").salesOrder(so)
                    .warehouse(warehouse).status(GoodsIssueStatus.DRAFT)
                    .type(GoodsIssueType.EXPORT_SALE).items(new ArrayList<>()).build();
            gin.getItems().add(GoodsIssueItem.builder().goodsIssueNote(gin).material(material())
                    .qty(10L).unitPrice(30000L).lineTotal(300000L).build());

            when(goodsIssueNoteRepository.findById(1)).thenReturn(Optional.of(gin));
            when(goodsIssueNoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(goodsIssueMapper.toResponse(any()))
                    .thenReturn(GoodsIssueResponse.builder().code("GIN-2026-0001").build());

            service.confirm(1, "admin");

            verify(inventoryService).decrease(eq(1), eq(1), eq(10L),
                    eq(StockRefType.GIN), eq("GIN-2026-0001"), eq("admin"));
            assertThat(so.getStatus()).isEqualTo(SalesOrderStatus.COMPLETED);
            assertThat(soLine.getIssuedQty()).isEqualTo(10L);
            verify(invoiceService).createSalesInvoice(any());
        }

        @Test
        @DisplayName("oversell rejected at GIN confirm (409 detail from inventory)")
        void oversell() {
            var so = SalesOrder.builder().id(1).code("SO-2026-0001").customer(customer()).customerName("Anh Ba")
                    .status(SalesOrderStatus.CONFIRMED).items(new ArrayList<>()).build();
            var soLine = SalesOrderItem.builder().salesOrder(so).material(material())
                    .qty(200L).unitPrice(30000L).lineTotal(6000000L).issuedQty(0L).returnedQty(0L).build();
            so.getItems().add(soLine);
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var gin = GoodsIssueNote.builder().id(1).code("GIN-2026-0001").salesOrder(so)
                    .warehouse(warehouse).status(GoodsIssueStatus.DRAFT)
                    .type(GoodsIssueType.EXPORT_SALE).items(new ArrayList<>()).build();
            gin.getItems().add(GoodsIssueItem.builder().goodsIssueNote(gin).material(material())
                    .qty(200L).unitPrice(30000L).lineTotal(6000000L).build());

            when(goodsIssueNoteRepository.findById(1)).thenReturn(Optional.of(gin));
            // kho chỉ còn 100 -> InventoryService ném thiếu hàng
            org.mockito.Mockito.doThrow(new BusinessValidationException(
                    "Insufficient stock for material 'VT-001': requested=200, available=100"))
                    .when(inventoryService).decrease(eq(1), eq(1), eq(200L),
                            eq(StockRefType.GIN), eq("GIN-2026-0001"), eq("admin"));

            assertThatThrownBy(() -> service.confirm(1, "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Insufficient stock");
            verify(invoiceService, never()).createSalesInvoice(any());
        }
    }

    @Nested
    @DisplayName("confirm() EXPORT_RETURN:")
    class ConfirmReturn {

        @Test
        @DisplayName("success Stock+ , no invoice, returnedQty+")
        void success() {
            var so = SalesOrder.builder().id(1).code("SO-2026-0001").customer(customer()).customerName("Anh Ba")
                    .status(SalesOrderStatus.COMPLETED).items(new ArrayList<>()).build();
            var soLine = SalesOrderItem.builder().salesOrder(so).material(material())
                    .qty(10L).unitPrice(30000L).lineTotal(300000L).issuedQty(10L).returnedQty(0L).build();
            so.getItems().add(soLine);
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var gin = GoodsIssueNote.builder().id(2).code("GIN-2026-0002").salesOrder(so)
                    .warehouse(warehouse).status(GoodsIssueStatus.DRAFT)
                    .type(GoodsIssueType.EXPORT_RETURN).items(new ArrayList<>()).build();
            gin.getItems().add(GoodsIssueItem.builder().goodsIssueNote(gin).material(material())
                    .qty(3L).unitPrice(30000L).lineTotal(90000L).build());

            when(goodsIssueNoteRepository.findById(2)).thenReturn(Optional.of(gin));
            when(goodsIssueNoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(goodsIssueMapper.toResponse(any()))
                    .thenReturn(GoodsIssueResponse.builder().code("GIN-2026-0002").build());

            service.confirm(2, "admin");

            verify(inventoryService).increase(eq(1), eq(1), eq(3L),
                    eq(StockRefType.GIN), eq("GIN-2026-0002"), eq("admin"));
            assertThat(soLine.getReturnedQty()).isEqualTo(3L);
            verify(invoiceService, never()).createSalesInvoice(any());
        }

        @Test
        @DisplayName("reject return beyond net issued")
        void overReturn() {
            var so = SalesOrder.builder().id(1).code("SO-2026-0001").customer(customer()).customerName("Anh Ba")
                    .status(SalesOrderStatus.COMPLETED).items(new ArrayList<>()).build();
            so.getItems().add(SalesOrderItem.builder().salesOrder(so).material(material())
                    .qty(10L).unitPrice(30000L).lineTotal(300000L).issuedQty(10L).returnedQty(8L).build());
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();

            // create() path validate trước khi lưu — dựng request vượt 10-8=2
            when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse));
            when(salesOrderRepository.findById(1)).thenReturn(Optional.of(so));

            assertThatThrownBy(() -> service.create(
                    com.ecommerce.sportcenter.module.sales.dto.request.CreateGoodsIssueRequest.builder()
                            .salesOrderId(1).warehouseId(1)
                            .type(GoodsIssueType.EXPORT_RETURN)
                            .items(List.of(com.ecommerce.sportcenter.module.sales.dto.request.CreateGoodsIssueRequest.GoodsIssueItemRequest.builder()
                                    .materialId(1).qty(5L).build()))
                            .build(), "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Return exceeds net issued");
        }
    }

    @Nested
    @DisplayName("invoice():")
    class ManualInvoice {

        @Test
        @DisplayName("success rebuild SALES invoice for CONFIRMED GIN")
        void success() {
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var gin = GoodsIssueNote.builder().id(7).code("GIN-2026-0007").customer(customer())
                    .customerName("Anh Ba").warehouse(warehouse).status(GoodsIssueStatus.CONFIRMED)
                    .type(GoodsIssueType.EXPORT_SALE).items(new ArrayList<>()).build();
            gin.getItems().add(GoodsIssueItem.builder().goodsIssueNote(gin).material(material())
                    .qty(4L).unitPrice(30000L).lineTotal(120000L).build());
            when(goodsIssueNoteRepository.findById(7)).thenReturn(Optional.of(gin));
            when(invoiceService.existsActiveInvoice("GIN-2026-0007", InvoiceType.SALES)).thenReturn(false);
            when(invoiceService.createSalesInvoice(any()))
                    .thenReturn(InvoiceResponse.builder().id(9).code("INV-2026-00009").build());

            assertThat(service.invoice(7).getCode()).isEqualTo("INV-2026-00009");
            verify(invoiceService).createSalesInvoice(any());
        }

        @Test
        @DisplayName("reject duplicate invoice for same GIN")
        void duplicate() {
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var gin = GoodsIssueNote.builder().id(7).code("GIN-2026-0007").customer(customer())
                    .customerName("Anh Ba").warehouse(warehouse).status(GoodsIssueStatus.CONFIRMED)
                    .type(GoodsIssueType.EXPORT_SALE).items(new ArrayList<>()).build();
            when(goodsIssueNoteRepository.findById(7)).thenReturn(Optional.of(gin));
            when(invoiceService.existsActiveInvoice("GIN-2026-0007", InvoiceType.SALES)).thenReturn(true);

            assertThatThrownBy(() -> service.invoice(7))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already has an active SALES invoice");
            verify(invoiceService, never()).createSalesInvoice(any());
        }
    }
}
