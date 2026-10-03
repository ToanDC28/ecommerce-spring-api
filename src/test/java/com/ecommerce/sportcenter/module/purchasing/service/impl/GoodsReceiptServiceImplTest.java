package com.ecommerce.sportcenter.module.purchasing.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
import com.ecommerce.sportcenter.module.inventory.entity.Warehouse;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreateGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.inventory.repository.WarehouseRepository;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.purchasing.dto.mapper.GoodsReceiptMapper;
import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptResponse;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptItem;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptNote;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptType;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrder;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderItem;
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
class GoodsReceiptServiceImplTest {

    @Mock
    private GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private GoodsReceiptMapper goodsReceiptMapper;

    @InjectMocks
    private GoodsReceiptServiceImpl service;

    private Material material() {
        return Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                .costPrice(25000L).sellPrice(30000L).stockQty(0L).active(true).build();
    }

    @Nested
    @DisplayName("confirm():")
    class Confirm {

        @Test
        @DisplayName("success Stock+ , PO SENT -> PARTIAL, auto PURCHASE invoice")
        void success() {
            var supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
            var po = PurchaseOrder.builder().id(1).code("PO-2026-0001").supplier(supplier)
                    .status(PurchaseOrderStatus.SENT).items(new ArrayList<>()).build();
            var poLine = PurchaseOrderItem.builder().purchaseOrder(po).material(material())
                    .qty(100L).unitCost(25000L).lineTotal(2500000L).receivedQty(0L).build();
            po.getItems().add(poLine);
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var grn = GoodsReceiptNote.builder().id(1).code("GRN-2026-0001").purchaseOrder(po)
                    .warehouse(warehouse).supplier(supplier).status(GoodsReceiptStatus.DRAFT)
                    .type(GoodsReceiptType.IMPORT_PURCHASE).items(new ArrayList<>()).build();
            grn.getItems().add(GoodsReceiptItem.builder().goodsReceiptNote(grn).material(material())
                    .qty(60L).unitCost(25000L).lineTotal(1500000L).build());

            when(goodsReceiptNoteRepository.findById(1)).thenReturn(Optional.of(grn));
            when(goodsReceiptNoteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(goodsReceiptMapper.toResponse(any())).thenReturn(GoodsReceiptResponse.builder().code("GRN-2026-0001").build());

            service.confirm(1, "admin");

            verify(inventoryService).increase(eq(1), eq(1), eq(60L),
                    eq(StockRefType.GRN), eq("GRN-2026-0001"), eq("admin"));
            assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIAL);
            assertThat(poLine.getReceivedQty()).isEqualTo(60L);
            verify(invoiceService).createPurchaseInvoice(any());
        }

        @Test
        @DisplayName("reject over-receipt beyond PO qty")
        void overReceipt() {
            var supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
            var po = PurchaseOrder.builder().id(1).code("PO-2026-0001").supplier(supplier)
                    .status(PurchaseOrderStatus.PARTIAL).items(new ArrayList<>()).build();
            var poLine = PurchaseOrderItem.builder().purchaseOrder(po).material(material())
                    .qty(100L).unitCost(25000L).lineTotal(2500000L).receivedQty(80L).build();
            po.getItems().add(poLine);
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var grn = GoodsReceiptNote.builder().id(1).code("GRN-2026-0001").purchaseOrder(po)
                    .warehouse(warehouse).supplier(supplier).status(GoodsReceiptStatus.DRAFT)
                    .type(GoodsReceiptType.IMPORT_PURCHASE).items(new ArrayList<>()).build();
            grn.getItems().add(GoodsReceiptItem.builder().goodsReceiptNote(grn).material(material())
                    .qty(50L).unitCost(25000L).lineTotal(1250000L).build());

            when(goodsReceiptNoteRepository.findById(1)).thenReturn(Optional.of(grn));

            assertThatThrownBy(() -> service.confirm(1, "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Over-receipt");
            verify(inventoryService, never()).increase(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("quickImport():")
    class QuickImport {

        @Test
        @DisplayName("success mua trực tiếp 1 bước: tạo + confirm + Stock+ + invoice")
        void directSuccess() {
            var supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var mat = material();
            var stored = GoodsReceiptNote.builder().id(1).code("GRN-2026-0001").warehouse(warehouse)
                    .supplier(supplier).status(GoodsReceiptStatus.DRAFT)
                    .type(GoodsReceiptType.IMPORT_PURCHASE).items(new ArrayList<>()).build();
            stored.getItems().add(GoodsReceiptItem.builder().goodsReceiptNote(stored).material(mat)
                    .qty(20L).unitCost(25000L).lineTotal(500000L).build());
            when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(materialRepository.findById(1)).thenReturn(Optional.of(mat));
            when(goodsReceiptNoteRepository.count()).thenReturn(0L);
            when(goodsReceiptNoteRepository.existsByCode(any())).thenReturn(false);
            when(goodsReceiptNoteRepository.save(any())).thenAnswer(i -> {
                GoodsReceiptNote grn = i.getArgument(0);
                if (grn.getId() == 0) {
                    grn.setId(1);
                }
                return grn;
            });
            // confirm() tải lại phiếu — trả về phiếu đã có dòng hàng như create() vừa lưu
            when(goodsReceiptNoteRepository.findById(1)).thenReturn(Optional.of(stored));
            when(goodsReceiptMapper.toResponse(any()))
                    .thenReturn(GoodsReceiptResponse.builder().id(1).code("GRN-2026-0001").build());

            var result = service.quickImport(CreateGoodsReceiptRequest.builder()
                    .warehouseId(1).supplierId(1)
                    .items(List.of(CreateGoodsReceiptRequest.GoodsReceiptItemRequest.builder()
                            .materialId(1).qty(20L).unitCost(25000L).build()))
                    .build(), "admin");

            assertThat(result.getCode()).isEqualTo("GRN-2026-0001");
            verify(inventoryService).increase(eq(1), eq(1), eq(20L),
                    eq(StockRefType.GRN), eq("GRN-2026-0001"), eq("admin"));
            verify(invoiceService).createPurchaseInvoice(any());
            assertThat(stored.getStatus()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("invoice():")
    class ManualInvoice {

        @Test
        @DisplayName("success rebuild PURCHASE invoice for CONFIRMED GRN")
        void success() {
            var supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var grn = GoodsReceiptNote.builder().id(5).code("GRN-2026-0005").warehouse(warehouse)
                    .supplier(supplier).status(GoodsReceiptStatus.CONFIRMED)
                    .type(GoodsReceiptType.IMPORT_PURCHASE).items(new ArrayList<>()).build();
            grn.getItems().add(GoodsReceiptItem.builder().goodsReceiptNote(grn).material(material())
                    .qty(10L).unitCost(25000L).batchNo("LOT-1").lineTotal(250000L).build());
            when(goodsReceiptNoteRepository.findById(5)).thenReturn(Optional.of(grn));
            when(invoiceService.existsActiveInvoice("GRN-2026-0005", InvoiceType.PURCHASE)).thenReturn(false);
            when(invoiceService.createPurchaseInvoice(any()))
                    .thenReturn(InvoiceResponse.builder().id(8).code("INV-2026-00008").build());

            assertThat(service.invoice(5).getCode()).isEqualTo("INV-2026-00008");
            verify(invoiceService).createPurchaseInvoice(any());
        }

        @Test
        @DisplayName("reject duplicate invoice for same GRN")
        void duplicate() {
            var supplier = Supplier.builder().id(1).code("SUP-001").name("ACME").active(true).build();
            var warehouse = Warehouse.builder().id(1).code("WH-XUONG-01").name("Kho xưởng").build();
            var grn = GoodsReceiptNote.builder().id(5).code("GRN-2026-0005").warehouse(warehouse)
                    .supplier(supplier).status(GoodsReceiptStatus.CONFIRMED)
                    .type(GoodsReceiptType.IMPORT_PURCHASE).items(new ArrayList<>()).build();
            when(goodsReceiptNoteRepository.findById(5)).thenReturn(Optional.of(grn));
            when(invoiceService.existsActiveInvoice("GRN-2026-0005", InvoiceType.PURCHASE)).thenReturn(true);

            assertThatThrownBy(() -> service.invoice(5))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("already has an active PURCHASE invoice");
            verify(invoiceService, never()).createPurchaseInvoice(any());
        }
    }
}
