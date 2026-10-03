package com.ecommerce.sportcenter.module.supplier.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.supplier.SupplierApiExamples;
import com.ecommerce.sportcenter.module.supplier.dto.request.CreateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.SearchSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.request.UpdateSupplierRequest;
import com.ecommerce.sportcenter.module.supplier.dto.response.SupplierResponse;
import com.ecommerce.sportcenter.module.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPPLIER_READ')")
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "Search and list suppliers (paginated)", description = "Keyword across code, name, phone with active filter")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suppliers retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SupplierList", value = SupplierApiExamples.VIEW_200))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_READ')")
    public ApiResponse<PageResponse<SupplierResponse>> search(@ParameterObject SearchSupplierRequest request) {
        PageResponse<SupplierResponse> page = supplierService.search(request);
        return ApiResponse.<PageResponse<SupplierResponse>>builder()
                .statusCode(200)
                .message("Suppliers retrieved successfully")
                .data(page)
                .build();
    }

    @Operation(summary = "Get supplier by id")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SupplierDetail", value = SupplierApiExamples.DETAIL_200)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_READ')")
    public ApiResponse<SupplierResponse> getById(@Parameter(description = "Supplier id") @PathVariable int id) {
        return ApiResponse.<SupplierResponse>builder()
                .statusCode(200)
                .message("Supplier retrieved successfully")
                .data(supplierService.getById(id))
                .build();
    }

    @Operation(summary = "Create supplier")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE', 'SUPPLIER_CREATE')")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse created = supplierService.create(request);
        return ApiResponse.<SupplierResponse>builder()
                .statusCode(201)
                .message("Supplier created successfully")
                .data(created)
                .build();
    }

    @Operation(summary = "Update supplier")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE', 'SUPPLIER_UPDATE')")
    public ApiResponse<SupplierResponse> update(@PathVariable int id,
                                                @Valid @RequestBody UpdateSupplierRequest request) {
        return ApiResponse.<SupplierResponse>builder()
                .statusCode(200)
                .message("Supplier updated successfully")
                .data(supplierService.update(id, request))
                .build();
    }

    @Operation(summary = "Enable/disable supplier")
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_WRITE', 'SUPPLIER_UPDATE')")
    public ApiResponse<SupplierResponse> setActive(@PathVariable int id,
                                                   @RequestBody Map<String, Boolean> body) {
        return ApiResponse.<SupplierResponse>builder()
                .statusCode(200)
                .message("Supplier status updated successfully")
                .data(supplierService.setActive(id, Boolean.TRUE.equals(body.get("active"))))
                .build();
    }
}
