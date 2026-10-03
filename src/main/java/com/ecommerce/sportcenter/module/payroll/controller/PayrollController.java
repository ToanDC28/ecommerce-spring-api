package com.ecommerce.sportcenter.module.payroll.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.payroll.PayrollApiExamples;
import com.ecommerce.sportcenter.module.payroll.dto.request.ApprovePayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.request.SearchPayrollRequest;
import com.ecommerce.sportcenter.module.payroll.dto.response.PayrollResponse;
import com.ecommerce.sportcenter.module.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payrolls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
public class PayrollController {

    private final PayrollService payrollService;
    private final SecurityService securityService;

    @Operation(summary = "Search payrolls (paginated; staff thường chỉ thấy của mình)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payrolls retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "PayrollList", value = PayrollApiExamples.VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<PayrollResponse>> search(
            @ParameterObject SearchPayrollRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        boolean canManageAll = securityService.hasAuthority("PAYROLL_WRITE");
        return ApiResponse.<PageResponse<PayrollResponse>>builder()
                .statusCode(200).message("Payrolls retrieved successfully")
                .data(payrollService.search(request, principal.getUsername(), canManageAll)).build();
    }

    @Operation(summary = "My payrolls")
    @GetMapping("/my")
    public ApiResponse<List<PayrollResponse>> my(
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.<List<PayrollResponse>>builder()
                .statusCode(200).message("My payrolls retrieved successfully")
                .data(payrollService.myPayrolls(principal.getUsername(), period)).build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
    public ApiResponse<PayrollResponse> getById(@PathVariable int id,
                                               @AuthenticationPrincipal UserDetails principal) {
        boolean canManageAll = securityService.hasAuthority("PAYROLL_WRITE");
        return ApiResponse.<PayrollResponse>builder()
                .statusCode(200).message("Payroll retrieved successfully")
                .data(payrollService.getById(id, principal.getUsername(), canManageAll)).build();
    }

    @Operation(summary = "Generate payrolls for period (idempotent, PENDING only)")
    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<List<PayrollResponse>> generate(@RequestParam String period) {
        return ApiResponse.<List<PayrollResponse>>builder()
                .statusCode(201).message("Payrolls generated successfully")
                .data(payrollService.generate(period)).build();
    }

    @Operation(summary = "Approve payroll (chốt thuế TNCN khi duyệt)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<PayrollResponse> approve(@PathVariable int id,
                                               @Valid @RequestBody(required = false) ApprovePayrollRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.<PayrollResponse>builder()
                .statusCode(200).message("Payroll approved")
                .data(payrollService.approve(id, request, principal.getUsername())).build();
    }

    @Operation(summary = "Reject payroll (về REJECTED, generate lại sẽ tính lại)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<PayrollResponse> reject(@PathVariable int id,
                                              @RequestBody(required = false) Map<String, String> body,
                                              @AuthenticationPrincipal UserDetails principal) {
        String note = body == null ? null : body.get("note");
        return ApiResponse.<PayrollResponse>builder()
                .statusCode(200).message("Payroll rejected")
                .data(payrollService.reject(id, note, principal.getUsername())).build();
    }

    @Operation(summary = "Pay payroll (APPROVED -> PAID, immutable)")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('PAYROLL_WRITE')")
    public ApiResponse<PayrollResponse> pay(@PathVariable int id,
                                           @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.<PayrollResponse>builder()
                .statusCode(200).message("Payroll paid")
                .data(payrollService.pay(id, principal.getUsername())).build();
    }
}
