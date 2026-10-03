package com.ecommerce.sportcenter.module.inventory.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.inventory.dto.request.CreateWarehouseRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.WarehouseResponse;
import com.ecommerce.sportcenter.module.inventory.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "List warehouses (tiệm nhỏ thường chỉ 1 kho)")
    @GetMapping
    public ApiResponse<List<WarehouseResponse>> getAll() {
        return ApiResponse.<List<WarehouseResponse>>builder()
                .statusCode(200).message("Warehouses retrieved successfully")
                .data(warehouseService.getAll()).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseResponse> getById(@PathVariable int id) {
        return ApiResponse.<WarehouseResponse>builder()
                .statusCode(200).message("Warehouse retrieved successfully")
                .data(warehouseService.getById(id)).build();
    }

    @Operation(summary = "Create warehouse")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.<WarehouseResponse>builder()
                .statusCode(201).message("Warehouse created successfully")
                .data(warehouseService.create(request)).build();
    }
}
