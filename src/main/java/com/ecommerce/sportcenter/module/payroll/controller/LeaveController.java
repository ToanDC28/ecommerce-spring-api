package com.ecommerce.sportcenter.module.payroll.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.payroll.dto.request.RecordLeaveRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.LeaveResponse;
import com.ecommerce.sportcenter.module.payroll.service.LeaveService;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
public class LeaveController {

    private final LeaveService leaveService;
    private final SecurityService securityService;

    @Operation(summary = "List leave records (thợ chỉ thấy của mình)")
    @GetMapping
    public ApiResponse<List<LeaveResponse>> list(
            @RequestParam(required = false) Integer staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails principal) {
        boolean canManageAll = securityService.hasAuthority("PAYROLL_WRITE");
        return ApiResponse.<List<LeaveResponse>>builder()
                .statusCode(200).message("Leaves retrieved successfully")
                .data(leaveService.list(staffId, from, to, principal.getUsername(), canManageAll)).build();
    }

    @Operation(summary = "Record a leave day (chấm nghỉ)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<LeaveResponse> record(@Valid @RequestBody RecordLeaveRequest request) {
        return ApiResponse.<LeaveResponse>builder()
                .statusCode(201).message("Leave recorded successfully")
                .data(leaveService.record(request)).build();
    }

    @Operation(summary = "Delete a leave record (nhập nhầm ngày)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<Void> delete(@PathVariable int id) {
        leaveService.delete(id);
        return ApiResponse.<Void>builder()
                .statusCode(200).message("Leave deleted successfully").build();
    }
}
