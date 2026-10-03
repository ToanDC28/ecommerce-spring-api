package com.ecommerce.sportcenter.module.payment.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.payment.PaymentApiExamples;
import com.ecommerce.sportcenter.module.payment.dto.request.CreatePaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchPaymentRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentSummaryResponse;
import com.ecommerce.sportcenter.module.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE', 'INVOICE_READ')")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Search payments (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "PaymentList", value = PaymentApiExamples.VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> search(@ParameterObject SearchPaymentRequest request) {
        return ApiResponse.<PageResponse<PaymentResponse>>builder()
                .statusCode(200).message("Payments retrieved successfully")
                .data(paymentService.search(request)).build();
    }

    @Operation(summary = "Daily close-out: totals by day x CASH/BANK")
    @GetMapping("/summary")
    public ApiResponse<PaymentSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.<PaymentSummaryResponse>builder()
                .statusCode(200).message("Payment summary retrieved successfully")
                .data(paymentService.summary(from, to)).build();
    }

    @Operation(summary = "Refund a payment (recomputes invoice paid status)")
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE')")
    public ApiResponse<PaymentResponse> refund(@PathVariable int id) {
        return ApiResponse.<PaymentResponse>builder()
                .statusCode(200).message("Payment refunded successfully")
                .data(paymentService.refund(id)).build();
    }
}
