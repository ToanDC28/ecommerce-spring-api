package com.ecommerce.sportcenter.module.payment.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.payment.dto.request.ApplyAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.CreateAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.request.SearchAdvanceRequest;
import com.ecommerce.sportcenter.module.payment.dto.response.AdvanceResponse;
import com.ecommerce.sportcenter.module.payment.dto.response.PaymentResponse;
import com.ecommerce.sportcenter.module.payment.service.AdvanceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/advances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE')")
public class AdvanceController {

    private final AdvanceService advanceService;
    private final SecurityService securityService;

    @Operation(summary = "List advances (cọc trước)")
    @GetMapping
    public ApiResponse<PageResponse<AdvanceResponse>> search(@ParameterObject SearchAdvanceRequest request) {
        return ApiResponse.<PageResponse<AdvanceResponse>>builder()
                .statusCode(200).message("Advances retrieved successfully")
                .data(advanceService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AdvanceResponse> getById(@PathVariable int id) {
        return ApiResponse.<AdvanceResponse>builder()
                .statusCode(200).message("Advance retrieved successfully")
                .data(advanceService.getById(id)).build();
    }

    @Operation(summary = "Record advance deposit (cọc hợp đồng/đơn hàng)")
    @PostMapping
    public ApiResponse<AdvanceResponse> create(@Valid @RequestBody CreateAdvanceRequest request) {
        return ApiResponse.<AdvanceResponse>builder()
                .statusCode(201).message("Advance recorded successfully")
                .data(advanceService.create(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Apply advance to invoice (cấn trừ toàn bộ, cùng khách)")
    @PostMapping("/{id}/apply")
    public ApiResponse<PaymentResponse> apply(@PathVariable int id,
                                             @Valid @RequestBody ApplyAdvanceRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .statusCode(200).message("Advance applied successfully")
                .data(advanceService.apply(id, request)).build();
    }

    @Operation(summary = "Cancel advance (chỉ ACTIVE, hoàn tiền ngoài hệ thống)")
    @PostMapping("/{id}/cancel")
    public ApiResponse<AdvanceResponse> cancel(@PathVariable int id) {
        return ApiResponse.<AdvanceResponse>builder()
                .statusCode(200).message("Advance cancelled")
                .data(advanceService.cancel(id)).build();
    }
}
