package com.ecommerce.sportcenter.module.invoice.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.invoice.InvoiceApiExamples;
import com.ecommerce.sportcenter.module.invoice.dto.request.CreateWorkInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.request.SearchInvoiceRequest;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INVOICE_READ')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Search invoices (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoices retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "InvoiceList", value = InvoiceApiExamples.VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<InvoiceResponse>> search(@ParameterObject SearchInvoiceRequest request) {
        return ApiResponse.<PageResponse<InvoiceResponse>>builder()
                .statusCode(200).message("Invoices retrieved successfully")
                .data(invoiceService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> getById(@PathVariable int id) {
        return ApiResponse.<InvoiceResponse>builder()
                .statusCode(200).message("Invoice retrieved successfully")
                .data(invoiceService.getById(id)).build();
    }

    @Operation(summary = "Create WORK invoice from WorkOrder DONE (actual materials + labor)")
    @PostMapping("/work")
    @PreAuthorize("hasAnyAuthority('INVOICE_WRITE')")
    public ApiResponse<InvoiceResponse> createWorkInvoice(@Valid @RequestBody CreateWorkInvoiceRequest request) {
        return ApiResponse.<InvoiceResponse>builder()
                .statusCode(201).message("Work invoice created successfully")
                .data(invoiceService.createWorkInvoice(request)).build();
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyAuthority('INVOICE_WRITE')")
    public ApiResponse<InvoiceResponse> issue(@PathVariable int id) {
        return ApiResponse.<InvoiceResponse>builder()
                .statusCode(200).message("Invoice issued")
                .data(invoiceService.issue(id)).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('INVOICE_WRITE')")
    public ApiResponse<InvoiceResponse> cancel(@PathVariable int id) {
        return ApiResponse.<InvoiceResponse>builder()
                .statusCode(200).message("Invoice cancelled")
                .data(invoiceService.cancel(id)).build();
    }
}
