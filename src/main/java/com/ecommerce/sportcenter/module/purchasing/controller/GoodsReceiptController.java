package com.ecommerce.sportcenter.module.purchasing.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.purchasing.PurchasingApiExamples;
import com.ecommerce.sportcenter.module.purchasing.dto.request.CreateGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.request.SearchGoodsReceiptRequest;
import com.ecommerce.sportcenter.module.purchasing.dto.response.GoodsReceiptResponse;
import com.ecommerce.sportcenter.module.purchasing.service.GoodsReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-receipts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;
    private final SecurityService securityService;

    @Operation(summary = "Search goods receipts (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goods receipts retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "GRNList", value = PurchasingApiExamples.GRN_VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<GoodsReceiptResponse>> search(@ParameterObject SearchGoodsReceiptRequest request) {
        return ApiResponse.<PageResponse<GoodsReceiptResponse>>builder()
                .statusCode(200).message("Goods receipts retrieved successfully")
                .data(goodsReceiptService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsReceiptResponse> getById(@PathVariable int id) {
        return ApiResponse.<GoodsReceiptResponse>builder()
                .statusCode(200).message("Goods receipt retrieved successfully")
                .data(goodsReceiptService.getById(id)).build();
    }

    @Operation(summary = "Create goods receipt (DRAFT, cân/đo thực tế)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsReceiptResponse> create(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        return ApiResponse.<GoodsReceiptResponse>builder()
                .statusCode(201).message("Goods receipt created successfully")
                .data(goodsReceiptService.create(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Confirm GRN (DRAFT -> CONFIRMED): Stock+ , update PO, auto PURCHASE invoice")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsReceiptResponse> confirm(@PathVariable int id) {
        return ApiResponse.<GoodsReceiptResponse>builder()
                .statusCode(200).message("Goods receipt confirmed, stock increased")
                .data(goodsReceiptService.confirm(id, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Nhập nhanh 1 bước (tạo + confirm ngay): hàng đã mua, phiếu ghi lịch sử")
    @PostMapping("/quick-import")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsReceiptResponse> quickImport(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        return ApiResponse.<GoodsReceiptResponse>builder()
                .statusCode(201).message("Goods imported successfully, stock increased")
                .data(goodsReceiptService.quickImport(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Xuất invoice PURCHASE tay cho GRN CONFIRMED (khi auto bị lỗi/mất)")
    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('INVOICE_WRITE')")
    public ApiResponse<com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse> invoice(@PathVariable int id) {
        return ApiResponse.<com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse>builder()
                .statusCode(201).message("Purchase invoice created successfully")
                .data(goodsReceiptService.invoice(id)).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsReceiptResponse> cancel(@PathVariable int id) {
        return ApiResponse.<GoodsReceiptResponse>builder()
                .statusCode(200).message("Goods receipt cancelled")
                .data(goodsReceiptService.cancel(id)).build();
    }
}
