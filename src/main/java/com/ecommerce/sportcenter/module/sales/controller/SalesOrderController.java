package com.ecommerce.sportcenter.module.sales.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.sales.SalesApiExamples;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchSalesOrderRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.SalesOrderResponse;
import com.ecommerce.sportcenter.module.sales.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ORDER_READ')")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final SecurityService securityService;

    @Operation(summary = "Search sales orders (paginated, material-only)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sales orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SOList", value = SalesApiExamples.SO_VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<SalesOrderResponse>> search(@ParameterObject SearchSalesOrderRequest request) {
        return ApiResponse.<PageResponse<SalesOrderResponse>>builder()
                .statusCode(200).message("Sales orders retrieved successfully")
                .data(salesOrderService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SalesOrderResponse> getById(@PathVariable int id) {
        return ApiResponse.<SalesOrderResponse>builder()
                .statusCode(200).message("Sales order retrieved successfully")
                .data(salesOrderService.getById(id)).build();
    }

    @Operation(summary = "Create sales order (PENDING, prices snapshot from sellPrice)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<SalesOrderResponse> create(@Valid @RequestBody CreateSalesOrderRequest request) {
        return ApiResponse.<SalesOrderResponse>builder()
                .statusCode(201).message("Sales order created successfully")
                .data(salesOrderService.create(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Confirm sales order (PENDING -> CONFIRMED, stock check)")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<SalesOrderResponse> confirm(@PathVariable int id) {
        return ApiResponse.<SalesOrderResponse>builder()
                .statusCode(200).message("Sales order confirmed")
                .data(salesOrderService.confirm(id)).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<SalesOrderResponse> cancel(@PathVariable int id) {
        return ApiResponse.<SalesOrderResponse>builder()
                .statusCode(200).message("Sales order cancelled")
                .data(salesOrderService.cancel(id)).build();
    }
}
