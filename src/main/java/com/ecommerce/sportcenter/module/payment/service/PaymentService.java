package com.ecommerce.sportcenter.module.payment.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchPaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {
    PaymentResponse pay(int invoiceId, CreatePaymentRequest request, String username);

    List<PaymentResponse> listByInvoice(int invoiceId);

    PageResponse<PaymentResponse> search(SearchPaymentRequest request);

    PaymentSummaryResponse summary(LocalDate from, LocalDate to);

    PaymentResponse refund(int paymentId);
}
