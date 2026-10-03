package com.ecommerce.sportcenter.module.payment.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.invoice.entity.Invoice;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceStatus;
import com.ecommerce.sportcenter.module.invoice.entity.InvoiceType;
import com.ecommerce.sportcenter.module.invoice.repository.InvoiceRepository;
import com.ecommerce.sportcenter.module.payment.dto.mapper.PaymentMapper;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.entity.Payment;
import com.ecommerce.sportcenter.module.payment.entity.PaymentMethod;
import com.ecommerce.sportcenter.module.payment.entity.PaymentStatus;
import com.ecommerce.sportcenter.module.payment.repository.PaymentRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl service;

    private Invoice issuedInvoice() {
        return Invoice.builder().id(1).code("INV-2026-00001").type(InvoiceType.WORK)
                .grandTotal(2000000L).paidAmount(0L).status(InvoiceStatus.ISSUED).build();
    }

    @Nested
    @DisplayName("pay():")
    class Pay {

        @Test
        @DisplayName("success partial CASH -> PARTIAL")
        void partialCash() {
            var invoice = issuedInvoice();
            when(invoiceRepository.findByIdForUpdate(1)).thenReturn(Optional.of(invoice));
            when(paymentRepository.count()).thenReturn(0L);
            when(paymentRepository.existsByCode(any())).thenReturn(false);
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponse(any())).thenReturn(PaymentResponse.builder().amount(500000L).build());

            var result = service.pay(1, CreatePaymentRequest.builder()
                    .amount(500000L).method(PaymentMethod.CASH).note("Trả 1 phần").build(), "cashier01");

            assertThat(result.getAmount()).isEqualTo(500000L);
            assertThat(invoice.getPaidAmount()).isEqualTo(500000L);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        }

        @Test
        @DisplayName("success full amount -> PAID")
        void fullPaid() {
            var invoice = issuedInvoice();
            invoice.setPaidAmount(1500000L);
            invoice.setStatus(InvoiceStatus.PARTIAL);
            when(invoiceRepository.findByIdForUpdate(1)).thenReturn(Optional.of(invoice));
            when(paymentRepository.count()).thenReturn(1L);
            when(paymentRepository.existsByCode(any())).thenReturn(false);
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentMapper.toResponse(any())).thenReturn(PaymentResponse.builder().amount(500000L).build());

            service.pay(1, CreatePaymentRequest.builder()
                    .amount(500000L).method(PaymentMethod.BANK_TRANSFER)
                    .transactionRef("MBVCB999").build(), "cashier01");

            assertThat(invoice.getPaidAmount()).isEqualTo(2000000L);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        @DisplayName("reject overpay")
        void overpay() {
            var invoice = issuedInvoice();
            when(invoiceRepository.findByIdForUpdate(1)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> service.pay(1, CreatePaymentRequest.builder()
                    .amount(3000000L).method(PaymentMethod.CASH).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Overpay");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("idempotent duplicate transactionRef returns existing")
        void idempotent() {
            var existing = Payment.builder().id(9).code("PAY-2026-0009").amount(500000L)
                    .method(PaymentMethod.BANK_TRANSFER).status(PaymentStatus.SUCCESS).build();
            var response = PaymentResponse.builder().id(9).amount(500000L).build();
            when(paymentRepository.findByTransactionRef("MBVCB999")).thenReturn(Optional.of(existing));
            when(paymentMapper.toResponse(existing)).thenReturn(response);

            assertThat(service.pay(1, CreatePaymentRequest.builder()
                    .amount(500000L).method(PaymentMethod.BANK_TRANSFER)
                    .transactionRef("MBVCB999").build(), "cashier01")).isEqualTo(response);
            verify(invoiceRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("reject BANK_TRANSFER without transactionRef")
        void bankNoRef() {
            assertThatThrownBy(() -> service.pay(1, CreatePaymentRequest.builder()
                    .amount(100000L).method(PaymentMethod.BANK_TRANSFER).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("Transaction ref");
        }

        @Test
        @DisplayName("reject payment on DRAFT invoice")
        void draftInvoice() {
            var draft = Invoice.builder().id(2).code("INV-2").type(InvoiceType.WORK)
                    .grandTotal(1000000L).paidAmount(0L).status(InvoiceStatus.DRAFT).build();
            when(invoiceRepository.findByIdForUpdate(2)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> service.pay(2, CreatePaymentRequest.builder()
                    .amount(100000L).method(PaymentMethod.CASH).build(), "cashier01"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("ISSUED/PARTIAL/OVERDUE");
        }
    }

    @Nested
    @DisplayName("refund():")
    class Refund {

        @Test
        @DisplayName("success full refund of PAID -> REFUNDED")
        void fullRefund() {
            var invoice = issuedInvoice();
            invoice.setPaidAmount(2000000L);
            invoice.setStatus(InvoiceStatus.PAID);
            var payment = Payment.builder().id(5).code("PAY-1").invoice(invoice)
                    .amount(2000000L).method(PaymentMethod.CASH).status(PaymentStatus.SUCCESS).build();
            when(paymentRepository.findById(5)).thenReturn(Optional.of(payment));
            when(invoiceRepository.findByIdForUpdate(1)).thenReturn(Optional.of(invoice));
            when(paymentMapper.toResponse(any())).thenReturn(PaymentResponse.builder().id(5).build());

            service.refund(5);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(invoice.getPaidAmount()).isEqualTo(0L);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REFUNDED);
        }

        @Test
        @DisplayName("reject refund of non-SUCCESS payment")
        void nonSuccess() {
            var payment = Payment.builder().id(6).code("PAY-2").amount(100L)
                    .method(PaymentMethod.CASH).status(PaymentStatus.REFUNDED).build();
            when(paymentRepository.findById(6)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.refund(6))
                    .isInstanceOf(BusinessValidationException.class);
        }
    }
}
