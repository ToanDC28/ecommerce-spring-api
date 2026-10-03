package com.ecommerce.sportcenter.module.sales.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.sales.SalesApiExamples;
import com.ecommerce.sportcenter.module.sales.dto.request.CreateGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.request.SearchGoodsIssueRequest;
import com.ecommerce.sportcenter.module.sales.dto.response.GoodsIssueResponse;
import com.ecommerce.sportcenter.module.sales.service.GoodsIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods-issues")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INVENTORY_READ')")
public class GoodsIssueController {

    private final GoodsIssueService goodsIssueService;
    private final SecurityService securityService;

    @Operation(summary = "Search goods issues (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goods issues retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "GINList", value = SalesApiExamples.GIN_VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<GoodsIssueResponse>> search(@ParameterObject SearchGoodsIssueRequest request) {
        return ApiResponse.<PageResponse<GoodsIssueResponse>>builder()
                .statusCode(200).message("Goods issues retrieved successfully")
                .data(goodsIssueService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<GoodsIssueResponse> getById(@PathVariable int id) {
        return ApiResponse.<GoodsIssueResponse>builder()
                .statusCode(200).message("Goods issue retrieved successfully")
                .data(goodsIssueService.getById(id)).build();
    }

    @Operation(summary = "Create goods issue (DRAFT)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsIssueResponse> create(@Valid @RequestBody CreateGoodsIssueRequest request) {
        return ApiResponse.<GoodsIssueResponse>builder()
                .statusCode(201).message("Goods issue created successfully")
                .data(goodsIssueService.create(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Confirm GIN (DRAFT -> CONFIRMED): Stock-, SO issued/status, auto SALES invoice (returns Stock+)")
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsIssueResponse> confirm(@PathVariable int id) {
        return ApiResponse.<GoodsIssueResponse>builder()
                .statusCode(200).message("Goods issue confirmed, stock updated")
                .data(goodsIssueService.confirm(id, securityService.getCurrentUsername())).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsIssueResponse> cancel(@PathVariable int id) {
        return ApiResponse.<GoodsIssueResponse>builder()
                .statusCode(200).message("Goods issue cancelled")
                .data(goodsIssueService.cancel(id)).build();
    }

    @Operation(summary = "Bán lẻ trực tiếp tại quầy 1 bước (tạo + confirm ngay, auto SALES invoice)")
    @PostMapping("/quick-sale")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<GoodsIssueResponse> quickSale(@Valid @RequestBody CreateGoodsIssueRequest request) {
        return ApiResponse.<GoodsIssueResponse>builder()
                .statusCode(201).message("Goods sold successfully, stock updated")
                .data(goodsIssueService.quickSale(request, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "Xuất invoice SALES tay cho GIN CONFIRMED (khi auto bị lỗi/mất)")
    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('INVOICE_WRITE')")
    public ApiResponse<com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse> invoice(@PathVariable int id) {
        return ApiResponse.<com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse>builder()
                .statusCode(201).message("Sales invoice created successfully")
                .data(goodsIssueService.invoice(id)).build();
    }
}
