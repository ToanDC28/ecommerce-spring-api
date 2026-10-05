package com.ecommerce.sportcenter.module.payroll.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpdatePayrollSettingRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollSettingResponse;
import com.ecommerce.sportcenter.module.payroll.service.PayrollSettingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
public class PayrollSettingController {

    private final PayrollSettingService payrollSettingService;

    @Operation(summary = "Get payroll settings (ngày nghỉ hợp lệ + công chuẩn)")
    @GetMapping
    public ApiResponse<PayrollSettingResponse> get() {
        return ApiResponse.<PayrollSettingResponse>builder()
                .statusCode(200).message("Payroll settings retrieved successfully")
                .data(payrollSettingService.get()).build();
    }

    @Operation(summary = "Update payroll settings (vd nghỉ T7+CN hay chỉ CN)")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<PayrollSettingResponse> update(@Valid @RequestBody UpdatePayrollSettingRequest request) {
        return ApiResponse.<PayrollSettingResponse>builder()
                .statusCode(200).message("Payroll settings updated successfully")
                .data(payrollSettingService.update(request)).build();
    }
}
