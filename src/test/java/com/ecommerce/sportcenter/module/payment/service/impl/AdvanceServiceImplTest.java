package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.customer.entity.Customer;
import com.ecommerce.sportcenter.module.customer.repository.CustomerRepository;
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
    private CustomerRepository customerRepository;
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
            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));
            when(advanceRepository.count()).thenReturn(0L);
            when(advanceRepository.existsByCode(any())).thenReturn(false);
            when(advanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(advanceMapper.toResponse(any()))
                    .thenReturn(AdvanceResponse.builder().code("ADV-2026-0001").build());

            var result = service.create(CreateAdvanceRequest.builder()
                    .customerId(1).amount(2000000L).method(PaymentMethod.CASH).note("Cọc 30%").build(), "cashier01");

            assertThat(result.getCode()).isEqualTo("ADV-2026-0001");
        }

        @Test
        @DisplayName("reject BANK without ref")
        void bankNoRef() {
            when(customerRepository.findById(1)).thenReturn(Optional.of(customer()));

            assertThatThrownBy(() -> service.create(CreateAdvanceRequest.builder()
                    .customerId(1).amount(2000000L).method(PaymentMethod.BANK_TRANSFER).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Transaction ref");
        }
    }

    @Nested
    @DisplayName("apply():")
    class Apply {

        @Test
        @DisplayName("success offsets invoice, marks APPLIED")
        void success() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-2026-0001").customer(customer())
                    .amount(2000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .customerId(1).customerName("Anh Ba")
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
        @DisplayName("reject different customer")
        void differentCustomer() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-1").customer(customer())
                    .amount(1000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .customerId(2).customerName("Chị Tư")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
            when(advanceRepository.findById(1)).thenReturn(Optional.of(advance));
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> service.apply(1, ApplyAdvanceRequest.builder().invoiceId(5).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("another customer");
            verify(paymentService, never()).pay(any(), any(), any());
        }

        @Test
        @DisplayName("reject when advance exceeds remaining")
        void exceedsRemaining() {
            var advance = AdvanceDeposit.builder().id(1).code("ADV-1").customer(customer())
                    .amount(9000000L).method(PaymentMethod.CASH).status(AdvanceStatus.ACTIVE).build();
            var invoice = Invoice.builder().id(5).code("INV-1").type(InvoiceType.WORK)
                    .customerId(1).customerName("Anh Ba")
                    .grandTotal(5000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
            when(advanceRepository.findById(1)).thenReturn(Optional.of(advance));
            when(invoiceRepository.findById(5)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> service.apply(1, ApplyAdvanceRequest.builder().invoiceId(5).build()))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("exceeds invoice remaining");
        }
    }
}
