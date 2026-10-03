package com.ecommerce.sportcenter.module.inventory.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.inventory.InventoryApiExamples;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockRequest;
import com.ecommerce.sportcenter.module.inventory.dto.request.SearchStockTransactionRequest;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockResponse;
import com.ecommerce.sportcenter.module.inventory.dto.response.StockTransactionResponse;
import com.ecommerce.sportcenter.module.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
public class StockController {

    private final InventoryService inventoryService;

    @Operation(summary = "Xem tồn kho trong kho (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stocks retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "StockList", value = InventoryApiExamples.STOCK_VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<StockResponse>> search(@ParameterObject SearchStockRequest request) {
        return ApiResponse.<PageResponse<StockResponse>>builder()
                .statusCode(200).message("Stocks retrieved successfully")
                .data(inventoryService.searchStocks(request)).build();
    }

    @Operation(summary = "Vật liệu sắp hết (tồn <= minStock)")
    @GetMapping("/low-stock")
    public ApiResponse<List<StockResponse>> lowStock(
            @RequestParam(required = false) Integer warehouseId) {
        return ApiResponse.<List<StockResponse>>builder()
                .statusCode(200).message("Low stocks retrieved successfully")
                .data(inventoryService.lowStock(warehouseId)).build();
    }

    @Operation(summary = "Lịch sử nhập/xuất kho (theo vật tư, kho, loại phiếu)")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<StockTransactionResponse>> transactions(
            @ParameterObject SearchStockTransactionRequest request) {
        return ApiResponse.<PageResponse<StockTransactionResponse>>builder()
                .statusCode(200).message("Stock transactions retrieved successfully")
                .data(inventoryService.searchTransactions(request)).build();
    }
}
