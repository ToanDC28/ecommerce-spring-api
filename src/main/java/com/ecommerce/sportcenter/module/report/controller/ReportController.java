package com.ecommerce.sportcenter.module.report.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.report.dto.response.DebtRow;
import com.ecommerce.sportcenter.module.report.dto.response.ProfitResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenueByTypeResponse;
import com.ecommerce.sportcenter.module.report.dto.response.RevenuePoint;
import com.ecommerce.sportcenter.module.report.dto.response.SalaryCostResponse;
import com.ecommerce.sportcenter.module.report.dto.response.StockValueResponse;
import com.ecommerce.sportcenter.module.report.dto.response.TopMaterialRow;
import com.ecommerce.sportcenter.module.report.dto.response.WorkOrderProfitRow;
import com.ecommerce.sportcenter.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Revenue over time (WORK + SALES paid/partial, group by day|month)")
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ')")
    public ApiResponse<List<RevenuePoint>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String groupBy) {
        return ApiResponse.<List<RevenuePoint>>builder()
                .statusCode(200).message("Revenue report retrieved successfully")
                .data(reportService.revenue(from, to, groupBy)).build();
    }

    @Operation(summary = "Revenue split WORK vs SALES")
    @GetMapping("/revenue-by-type")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ')")
    public ApiResponse<RevenueByTypeResponse> revenueByType(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.<RevenueByTypeResponse>builder()
                .statusCode(200).message("Revenue by type retrieved successfully")
                .data(reportService.revenueByType(from, to)).build();
    }

    @Operation(summary = "Top consumed/sold materials")
    @GetMapping("/top-materials")
    @PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
    public ApiResponse<List<TopMaterialRow>> topMaterials(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.<List<TopMaterialRow>>builder()
                .statusCode(200).message("Top materials retrieved successfully")
                .data(reportService.topMaterials(limit, from, to)).build();
    }

    @Operation(summary = "Stock value (qty * cost) + low-stock list")
    @GetMapping("/stock-value")
    @PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
    public ApiResponse<StockValueResponse> stockValue() {
        return ApiResponse.<StockValueResponse>builder()
                .statusCode(200).message("Stock value retrieved successfully")
                .data(reportService.stockValue()).build();
    }

    @Operation(summary = "Supplier debt (PURCHASE unpaid)")
    @GetMapping("/supplier-debt")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ')")
    public ApiResponse<List<DebtRow>> supplierDebt() {
        return ApiResponse.<List<DebtRow>>builder()
                .statusCode(200).message("Supplier debt retrieved successfully")
                .data(reportService.supplierDebt()).build();
    }

    @Operation(summary = "Customer debt (WORK + SALES unpaid, by snapshot name/phone)")
    @GetMapping("/customer-debt")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ')")
    public ApiResponse<List<DebtRow>> customerDebt() {
        return ApiResponse.<List<DebtRow>>builder()
                .statusCode(200).message("Customer debt retrieved successfully")
                .data(reportService.customerDebt()).build();
    }

    @Operation(summary = "Salary cost for period (PAID payrolls)")
    @GetMapping("/salary-cost")
    @PreAuthorize("hasAnyAuthority('PAYROLL_READ')")
    public ApiResponse<SalaryCostResponse> salaryCost(@RequestParam String period) {
        return ApiResponse.<SalaryCostResponse>builder()
                .statusCode(200).message("Salary cost retrieved successfully")
                .data(reportService.salaryCost(period)).build();
    }

    @Operation(summary = "Profit = revenue - material cost - salary")
    @GetMapping("/profit")
    @PreAuthorize("hasAnyAuthority('INVOICE_READ')")
    public ApiResponse<ProfitResponse> profit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.<ProfitResponse>builder()
                .statusCode(200).message("Profit report retrieved successfully")
                .data(reportService.profit(from, to)).build();
    }

    @Operation(summary = "Per-work-order profit (agreed/invoiced - actual - labor)")
    @GetMapping("/workorder-profit")
    @PreAuthorize("hasAnyAuthority('ORDER_READ')")
    public ApiResponse<List<WorkOrderProfitRow>> workOrderProfit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.<List<WorkOrderProfitRow>>builder()
                .statusCode(200).message("Work order profit retrieved successfully")
                .data(reportService.workOrderProfit(from, to)).build();
    }
}
