package com.ecommerce.sportcenter.module.payroll.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.payroll.dto.request.CreateSalaryGradeRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.SalaryGradeResponse;
import com.ecommerce.sportcenter.module.payroll.service.SalaryGradeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-grades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
public class SalaryGradeController {

    private final SalaryGradeService salaryGradeService;

    @Operation(summary = "List salary grades")
    @GetMapping
    public ApiResponse<List<SalaryGradeResponse>> getAll() {
        return ApiResponse.<List<SalaryGradeResponse>>builder()
                .statusCode(200).message("Salary grades retrieved successfully")
                .data(salaryGradeService.getAll()).build();
    }

    @Operation(summary = "Create salary grade")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<SalaryGradeResponse> create(@Valid @RequestBody CreateSalaryGradeRequest request) {
        return ApiResponse.<SalaryGradeResponse>builder()
                .statusCode(201).message("Salary grade created successfully")
                .data(salaryGradeService.create(request)).build();
    }
}
