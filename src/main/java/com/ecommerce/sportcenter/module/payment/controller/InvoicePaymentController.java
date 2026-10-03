package com.ecommerce.sportcenter.module.payment.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.payment.PaymentApiExamples;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE', 'INVOICE_READ')")
public class InvoicePaymentController {

    private final PaymentService paymentService;
    private final SecurityService securityService;

    @Operation(summary = "Record a payment on an invoice (CASH at counter / BANK_TRANSFER, partial allowed)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment recorded successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "Payment", value = PaymentApiExamples.CREATE_201)))
    })
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE')")
    public ApiResponse<PaymentResponse> pay(@PathVariable int id,
                                           @Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .statusCode(201).message("Payment recorded successfully")
                .data(paymentService.pay(id, request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "List payments of an invoice")
    @GetMapping("/{id}/payments")
    public ApiResponse<List<PaymentResponse>> listByInvoice(@PathVariable int id) {
        return ApiResponse.<List<PaymentResponse>>builder()
                .statusCode(200).message("Invoice payments retrieved successfully")
                .data(paymentService.listByInvoice(id)).build();
    }
}
