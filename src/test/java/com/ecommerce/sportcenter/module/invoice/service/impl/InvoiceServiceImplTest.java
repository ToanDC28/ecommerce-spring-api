package com.ecommerce.sportcenter.module.invoice.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
import com.ecommerce.sportcenter.module.invoice.dto.mapper.InvoiceMapper;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreatePurchaseInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateSalesInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateWorkInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderMaterial;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderMaterialRepository workOrderMaterialRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private InvoiceMapper invoiceMapper;

    @InjectMocks
    private InvoiceServiceImpl service;

    @Nested
    @DisplayName("createWorkInvoice():")
    class CreateWork {

        @Test
        @DisplayName("success từ actual + labor, VAT 10%")
        void success() {
            var wo = WorkOrder.builder().id(1).code("WO-2026-0001").type(WorkOrderType.REPAIR)
                    .customerName("ABC").status(WorkOrderStatus.DONE).laborCost(2000000L).overheadCost(0L).build();
            var material = Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                    .costPrice(25000L).sellPrice(30000L).build();
            var line = WorkOrderMaterial.builder().workOrder(wo).material(material)
                    .qtyPlanned(10L).qtyActual(5L).unitCost(25000L).unitSellPrice(30000L).build();

            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));
            when(workOrderMaterialRepository.findByWorkOrder_Id(1)).thenReturn(List.of(line));
            when(invoiceRepository.count()).thenReturn(0L);
            when(invoiceRepository.existsByCode(any())).thenReturn(false);
            when(invoiceRepository.save(any())).thenAnswer(i -> {
                Invoice inv = i.getArgument(0);
                if (inv.getId() == 0) {
                    inv.setId(1);
                }
                if (inv.getItems() == null) {
                    inv.setItems(new ArrayList<>());
                }
                return inv;
            });
            when(invoiceMapper.toResponse(any())).thenReturn(InvoiceResponse.builder().id(1).code("INV-2026-00001").build());

            var result = service.createWorkInvoice(CreateWorkInvoiceRequest.builder()
                    .workOrderId(1).vatRate(10).discountAmount(0L).build());

            assertThat(result.getCode()).isEqualTo("INV-2026-00001");
            // subTotal = 5*30000 + 2000000 = 2150000, VAT 10% = 215000
            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.INVOICED);
        }

        @Test
        @DisplayName("reject khi WO chưa DONE")
        void rejectNotDone() {
            var wo = WorkOrder.builder().id(1).code("WO-1").type(WorkOrderType.REPAIR)
                    .customerName("ABC").status(WorkOrderStatus.IN_PROGRESS).build();
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() -> service.createWorkInvoice(CreateWorkInvoiceRequest.builder()
                    .workOrderId(1).vatRate(10).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("DONE");
        }
    }

    @Nested
    @DisplayName("createPurchaseInvoice():")
    class CreatePurchase {

        @Test
        @DisplayName("success auto DRAFT from GRN lines, VAT 10%")
        void success() {
            when(invoiceRepository.count()).thenReturn(5L);
            when(invoiceRepository.existsByCode(any())).thenReturn(false);
            when(invoiceRepository.save(any())).thenAnswer(i -> {
                Invoice inv = i.getArgument(0);
                if (inv.getId() == 0) {
                    inv.setId(2);
                }
                if (inv.getItems() == null) {
                    inv.setItems(new ArrayList<>());
                }
                return inv;
            });
            when(invoiceMapper.toResponse(any())).thenReturn(InvoiceResponse.builder().id(2).code("INV-2026-00006").build());

            var result = service.createPurchaseInvoice(CreatePurchaseInvoiceRequest.builder()
                    .supplierId(1).supplierName("ACME").refCode("GRN-2026-0001")
                    .items(List.of(CreatePurchaseInvoiceRequest.PurchaseInvoiceLine.builder()
                            .materialId(1).description("VT-001 - Thép").qty(100L).unitCost(25000L).build()))
                    .vatRate(10).build());

            assertThat(result.getCode()).isEqualTo("INV-2026-00006");
            // subTotal = 100*25000 = 2500000, VAT 10% = 250000
        }

        @Test
        @DisplayName("reject bad VAT rate")
        void badVat() {
            assertThatThrownBy(() -> service.createPurchaseInvoice(CreatePurchaseInvoiceRequest.builder()
                    .supplierId(1).supplierName("ACME").refCode("GRN-1")
                    .items(List.of(CreatePurchaseInvoiceRequest.PurchaseInvoiceLine.builder()
                            .materialId(1).description("VT").qty(1L).unitCost(1000L).build()))
                    .vatRate(5).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("VAT");
        }
    }

    @Nested
    @DisplayName("createSalesInvoice():")
    class CreateSales {

        @Test
        @DisplayName("success auto DRAFT from GIN lines, VAT 0")
        void success() {
            var customer = Customer.builder().id(1).code("KH-001").name("Anh Ba")
                    .phone("0901234567").active(true).build();
            when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
            when(invoiceRepository.count()).thenReturn(10L);
            when(invoiceRepository.existsByCode(any())).thenReturn(false);
            when(invoiceRepository.save(any())).thenAnswer(i -> {
                Invoice inv = i.getArgument(0);
                if (inv.getId() == 0) {
                    inv.setId(3);
                }
                if (inv.getItems() == null) {
                    inv.setItems(new ArrayList<>());
                }
                return inv;
            });
            when(invoiceMapper.toResponse(any())).thenReturn(InvoiceResponse.builder().id(3).code("INV-2026-00011").build());

            var result = service.createSalesInvoice(CreateSalesInvoiceRequest.builder()
                    .soId(1).soCode("SO-2026-0001").customerId(1).customerName("Anh Ba")
                    .refCode("GIN-2026-0001")
                    .items(List.of(CreateSalesInvoiceRequest.SalesInvoiceLine.builder()
                            .materialId(1).description("VT-001 - Thép").qty(5L).unitPrice(30000L).build()))
                    .vatRate(0).build());

            assertThat(result.getCode()).isEqualTo("INV-2026-00011");
            // subTotal = 5*30000 = 150000, VAT 0% = 0
        }
    }

    @Nested
    @DisplayName("existsActiveInvoice() + markOverdue():")
    class GuardsAndJob {

        @Test
        @DisplayName("existsActiveInvoice false for null refCode")
        void nullRef() {
            assertThat(service.existsActiveInvoice(null, InvoiceType.SALES)).isFalse();
            verify(invoiceRepository, never()).existsByRefCodeAndTypeAndStatusNot(any(), any(), any());
        }

        @Test
        @DisplayName("markOverdue flips past-due ISSUED/PARTIAL to OVERDUE")
        void overdue() {
            var past = java.time.LocalDate.now().minusDays(3);
            var issued = Invoice.builder().id(1).code("INV-1").type(InvoiceType.SALES)
                    .grandTotal(1000L).status(InvoiceStatus.ISSUED).dueDate(past).build();
            var partial = Invoice.builder().id(2).code("INV-2").type(InvoiceType.WORK)
                    .grandTotal(2000L).paidAmount(500L).status(InvoiceStatus.PARTIAL).dueDate(past).build();
            when(invoiceRepository.findByStatusInAndDueDateBefore(
                    java.util.List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL),
                    java.time.LocalDate.now())).thenReturn(new ArrayList<>(java.util.List.of(issued, partial)));

            assertThat(service.markOverdue()).isEqualTo(2);
            assertThat(issued.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
            assertThat(partial.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
            verify(invoiceRepository).saveAll(any());
        }

        @Test
        @DisplayName("markOverdue no-op when nothing past due")
        void noneOverdue() {
            when(invoiceRepository.findByStatusInAndDueDateBefore(any(), any())).thenReturn(java.util.List.of());

            assertThat(service.markOverdue()).isEqualTo(0);
            verify(invoiceRepository, never()).saveAll(any());
        }
    }
}
