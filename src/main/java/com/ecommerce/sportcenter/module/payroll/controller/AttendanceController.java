package com.ecommerce.sportcenter.module.payroll.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.payroll.dto.request.UpsertAttendanceRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.AttendanceResponse;
import com.ecommerce.sportcenter.module.payroll.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "List attendances by period/staff")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
    public ApiResponse<List<AttendanceResponse>> list(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer staffId) {
        return ApiResponse.<List<AttendanceResponse>>builder()
                .statusCode(200).message("Attendances retrieved successfully")
                .data(attendanceService.list(period, staffId)).build();
    }

    @Operation(summary = "Record/update monthly attendance (chấm công + bậc lương tháng đó)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<AttendanceResponse> upsert(@Valid @RequestBody UpsertAttendanceRequest request) {
        return ApiResponse.<AttendanceResponse>builder()
                .statusCode(200).message("Attendance recorded successfully")
                .data(attendanceService.upsert(request)).build();
    }
}
