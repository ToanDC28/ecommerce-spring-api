package com.ecommerce.sportcenter.module.report.service.impl;

import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.material.entity.Material;
import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import com.ecommerce.sportcenter.module.material.repository.MaterialRepository;
import com.ecommerce.sportcenter.module.payroll.repository.PayrollRepository;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderItemRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderMaterialRepository;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderMaterialRepository workOrderMaterialRepository;
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private PayrollRepository payrollRepository;

    @InjectMocks
    private ReportServiceImpl service;

    private Invoice workInvoice(LocalDate issueDate, long grand) {
        return Invoice.builder().id(1).code("INV-W").type(InvoiceType.WORK)
                .customerName("ABC").issueDate(issueDate)
                .grandTotal(grand).paidAmount(0L).status(InvoiceStatus.PAID).build();
    }

    private Invoice salesInvoice(LocalDate issueDate, long grand) {
        return Invoice.builder().id(2).code("INV-S").type(InvoiceType.SALES)
                .customerName("Anh Ba").issueDate(issueDate)
                .grandTotal(grand).paidAmount(0L).status(InvoiceStatus.PARTIAL).build();
    }

    @Nested
    @DisplayName("revenue():")
    class Revenue {

        @Test
        @DisplayName("groups by day, WORK+SALES only")
        void groupsByDay() {
            when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(
                    workInvoice(LocalDate.of(2026, 9, 1), 1000000L),
                    salesInvoice(LocalDate.of(2026, 9, 1), 500000L),
                    workInvoice(LocalDate.of(2026, 9, 2), 2000000L)));

            var points = service.revenue(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), "day");

            assertThat(points).hasSize(2);
            assertThat(points.get(0).getLabel()).isEqualTo("2026-09-01");
            assertThat(points.get(0).getTotal()).isEqualTo(1500000L);
            assertThat(points.get(1).getTotal()).isEqualTo(2000000L);
        }

        @Test
        @DisplayName("groups by month")
        void groupsByMonth() {
            when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(
                    workInvoice(LocalDate.of(2026, 9, 1), 1000000L),
                    workInvoice(LocalDate.of(2026, 10, 5), 2000000L)));

            var points = service.revenue(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), "month");

            assertThat(points).hasSize(2);
            assertThat(points.get(0).getLabel()).isEqualTo("2026-09");
        }
    }

    @Nested
    @DisplayName("stockValue():")
    class StockValue {

        @Test
        @DisplayName("sums qty*cost and flags low stock")
        void sumsAndFlags() {
            when(materialRepository.findAll()).thenReturn(List.of(
                    Material.builder().id(1).sku("VT-001").name("Thép").unit(MaterialUnit.KG)
                            .costPrice(25000L).stockQty(100L).minStock(10L).active(true).build(),
                    Material.builder().id(2).sku("VT-002").name("Ốc").unit(MaterialUnit.CAI)
                            .costPrice(1000L).stockQty(5L).minStock(50L).active(true).build()));

            var result = service.stockValue();

            assertThat(result.getTotalValue()).isEqualTo(100L * 25000L + 5L * 1000L);
            assertThat(result.getMaterialCount()).isEqualTo(2);
            assertThat(result.getLowStock()).hasSize(1);
            assertThat(result.getLowStock().get(0).getSku()).isEqualTo("VT-002");
        }
    }

    @Nested
    @DisplayName("debts:")
    class Debts {

        @Test
        @DisplayName("supplierDebt groups PURCHASE open by supplier")
        void supplier() {
            when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(
                    Invoice.builder().id(1).code("I1").type(InvoiceType.PURCHASE)
                            .supplierName("ACME").issueDate(LocalDate.now())
                            .grandTotal(1000000L).paidAmount(400000L).status(InvoiceStatus.PARTIAL).build(),
                    Invoice.builder().id(2).code("I2").type(InvoiceType.PURCHASE)
                            .supplierName("ACME").issueDate(LocalDate.now())
                            .grandTotal(500000L).paidAmount(500000L).status(InvoiceStatus.PAID).build()));

            var rows = service.supplierDebt();

            // I2 fully paid (owed=0) is skipped; I1 owes 600k
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getTotalOwed()).isEqualTo(600000L);
            assertThat(rows.get(0).getInvoiceCount()).isEqualTo(1L);
        }
    }
}
