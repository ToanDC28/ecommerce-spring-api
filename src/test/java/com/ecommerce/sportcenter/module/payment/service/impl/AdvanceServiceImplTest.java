package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.sales.repository.SalesOrderRepository;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrder;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.repository.WorkOrderRepository;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.payment.dto.mapper.AdvanceMapper;
import com.ecommerce.sportcenter.module.payment.dto.request.ApplyAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.CreateAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.AdvanceResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceDeposit;
import com.ecommerce.sportcenter.module.payment.entity.AdvanceStatus;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.repository.AdvanceDepositRepository;
import com.ecommerce.sportcenter.module.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvanceServiceImplTest {

    @Mock
    private AdvanceDepositRepository advanceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private AdvanceMapper advanceMapper;

    @InjectMocks
    private AdvanceServiceImpl service;

    private Customer customer() {
        return Customer.builder().id(1).code("KH-001").name("Anh Ba")
                .phone("0901234567").active(true).build();
    }

    @Nested
    @DisplayName("create():")
    class Create {

        @Test
        @DisplayName("success cash advance")
        void success() {
            var wo = WorkOrder.builder().id(1).code("WO-2026-0001")
                    .type(com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType.REPAIR)
                    .customer(customer()).customerName("Anh Ba")
                    .status(WorkOrderStatus.CONFIRMED).build();
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));
            when(advanceRepository.count()).thenReturn(0L);
            when(advanceRepository.existsByCode(any())).thenReturn(false);
            when(advanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(advanceMapper.toResponse(any()))
                    .thenReturn(AdvanceResponse.builder().code("ADV-2026-0001").build());

            var result = service.create(CreateAdvanceRequest.builder()
                    .workOrderId(1).amount(2000000L).method(PaymentMethod.CASH).note("Cọc 30%").build(), "cashier01");

            assertThat(result.getCode()).isEqualTo("ADV-2026-0001");
        }

        @Test
        @DisplayName("reject BANK without ref")
        void bankNoRef() {
            var wo = WorkOrder.builder().id(1).code("WO-2026-0001")
                    .type(com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType.REPAIR)
                    .customer(customer()).customerName("Anh Ba")
                    .status(WorkOrderStatus.CONFIRMED).build();
            when(workOrderRepository.findById(1)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() -> service.create(CreateAdvanceRequest.builder()
                    .workOrderId(1).amount(2000000L).method(PaymentMethod.BANK_TRANSFER).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Transaction ref");
        }
    }

        @Test
        @DisplayName("reject when neither/both orders linked")
        void xorLink() {
            assertThatThrownBy(() -> service.create(CreateAdvanceRequest.builder()
                    .amount(1000000L).method(PaymentMethod.CASH).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("exactly one");
            assertThatThrownBy(() -> service.create(CreateAdvanceRequest.builder()
                    .workOrderId(1).salesOrderId(2).amount(1000000L).method(PaymentMethod.CASH).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("exactly one");
        }
    }

    @Nested
    @DisplayName("apply():")
    class Apply {

        @Test
        @DisplayName("success offsets invoice, marks APPLIED")
        void success() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-2026-0001").customer(customer())
                    .workOrderId(1).workOrderCode("WO-2026-0001")
                    .amount(2000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .workOrderId(1).workOrderCode("WO-2026-0001").customerId(1).customerName("Anh Ba")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
            when(advanceRepository.findById(1)).thenReturn(Optional.of(advance));
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));
            when(paymentService.pay(eq(5), any(), any()))
                    .thenReturn(PaymentResponse.builder().amount(2000000L).build());

            service.apply(1, ApplyAdvanceRequest.builder().invoiceId(5).build());

            assertThat(advance.getStatus()).isEqualTo(AdvanceStatus.APPLIED);
            assertThat(advance.getAppliedInvoiceId()).isEqualTo(5);
            verify(paymentService).pay(eq(5), any(), any());
        }

        @Test
        @DisplayName("reject invoice of another order")
        void differentOrder() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-1").customer(customer())
                    .workOrderId(1).workOrderCode("WO-2026-0001")
                    .amount(1000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .workOrderId(2).workOrderCode("WO-2026-0002").customerId(1).customerName("Anh Ba")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
            when(advanceRepository.findById(1)).thenReturn(Optional.of(advance));
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> service.apply(1, ApplyAdvanceRequest.builder().invoiceId(5).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("belongs to work order WO-2026-0001");
            verify(paymentService, never()).pay(any(), any(), any());
        }

        @Test
        @DisplayName("reject when advance exceeds remaining")
        void exceedsRemaining() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-1").customer(customer())
                    .workOrderId(1).workOrderCode("WO-2026-0001")
                    .amount(9000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .workOrderId(1).workOrderCode("WO-2026-0001").customerId(1).customerName("Anh Ba")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
            when(advanceRepository.findById(1)).thenReturn(Optional.of(advance));
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> service.apply(1, ApplyAdvanceRequest.builder().invoiceId(5).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("exceeds invoice remaining");
        }
    }

    @Nested
    @DisplayName("autoApply():")
    class AutoApply {

        @Test
        @DisplayName("applies fitting advances, skips oversized, stops when paid")
        void fittingOnly() {
            var fit = AdvanceDeposit.builder().id(1).code("ADV-1").customer(customer())
                    .workOrderId(1).workOrderCode("WO-2026-0001")
                    .amount(2000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var big = AdvanceDeposit.builder().id(2).code("ADV-2").customer(customer())
                    .workOrderId(1).workOrderCode("WO-2026-0001")
                    .amount(9000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .workOrderId(1).workOrderCode("WO-2026-0001").customerId(1).customerName("Anh Ba")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.DRAFT).build();
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));
            when(advanceRepository.findByWorkOrderIdAndStatusOrderByIdAsc(1, AdvanceStatus.ACTIVE))
                    .thenReturn(new java.util.ArrayList<>(java.util.List.of(fit, big)));
            when(advanceRepository.findById(1)).thenReturn(Optional.of(fit));
            when(paymentService.pay(eq(5), any(), any())).thenAnswer(i -> {
                // mô phỏng pay(): cộng paidAmount như thật để vòng sau thấy còn nợ giảm
                invoice.setPaidAmount(invoice.getPaidAmount() + 2000000L);
                return PaymentResponse.builder().amount(2000000L).build();
            });

            assertThat(service.autoApply(5)).isEqualTo(1);
            assertThat(fit.getStatus()).isEqualTo(AdvanceStatus.APPLIED);
            assertThat(big.getStatus()).isEqualTo(AdvanceStatus.ACTIVE); // 9tr > còn nợ 3tr -> giữ lại
            verify(paymentService, org.mockito.Mockito.times(1)).pay(eq(5), any(), any());
        }

        @Test
        @DisplayName("no advances of the order -> 0")
        void none() {
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .workOrderId(9).workOrderCode("WO-9").customerId(1)
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.DRAFT).build();
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));
            when(advanceRepository.findByWorkOrderIdAndStatusOrderByIdAsc(9, AdvanceStatus.ACTIVE))
                    .thenReturn(java.util.List.of());

            assertThat(service.autoApply(5)).isEqualTo(0);
            verify(paymentService, never()).pay(any(), any(), any());
        }
    }
}
