package com.ecommerce.sportcenter.module.purchasing.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.purchasing.PurchasingApiExamples;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreatePurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchPurchaseOrderRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.PurchaseOrderResponse;
import com.ecommerce.sportcenter.module.purchasing.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPPLIER_READ')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SecurityService securityService;

    @Operation(summary = "Search purchase orders (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Purchase orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "POList", value = PurchasingApiExamples.PO_VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<PurchaseOrderResponse>> search(@ParameterObject SearchPurchaseOrderRequest request) {
        return ApiResponse.<PageResponse<PurchaseOrderResponse>>builder()
                .statusCode(200).message("Purchase orders retrieved successfully")
                .data(purchaseOrderService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrderResponse> getById(@PathVariable int id) {
        return ApiResponse.<PurchaseOrderResponse>builder()
                .statusCode(200).message("Purchase order retrieved successfully")
                .data(purchaseOrderService.getById(id)).build();
    }

    @Operation(summary = "Create purchase order (DRAFT)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE')")
    public ApiResponse<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ApiResponse.<PurchaseOrderResponse>builder()
                .statusCode(201).message("Purchase order created successfully")
                .data(purchaseOrderService.create(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Send purchase order to supplier (DRAFT -> SENT)")
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE')")
    public ApiResponse<PurchaseOrderResponse> send(@PathVariable int id) {
        return ApiResponse.<PurchaseOrderResponse>builder()
                .statusCode(200).message("Purchase order sent")
                .data(purchaseOrderService.send(id)).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE')")
    public ApiResponse<PurchaseOrderResponse> cancel(@PathVariable int id) {
        return ApiResponse.<PurchaseOrderResponse>builder()
                .statusCode(200).message("Purchase order cancelled")
                .data(purchaseOrderService.cancel(id)).build();
    }
}
