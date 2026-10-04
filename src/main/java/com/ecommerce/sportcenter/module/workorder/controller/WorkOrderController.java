package com.ecommerce.sportcenter.module.workorder.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.workorder.WorkOrderApiExamples;
import com.ecommerce.sportcenter.module.workorder.dto.request.ConsumeMaterialRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.CreateWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.request.SearchWorkOrderRequest;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderAttachmentResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import com.ecommerce.sportcenter.module.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ORDER_READ')")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final SecurityService securityService;

    @Operation(summary = "Search work orders (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Work orders retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "WorkOrderList", value = WorkOrderApiExamples.VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<WorkOrderResponse>> search(@ParameterObject SearchWorkOrderRequest request) {
        return ApiResponse.<PageResponse<WorkOrderResponse>>builder()
                .statusCode(200).message("Work orders retrieved successfully")
                .data(workOrderService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderResponse> getById(@PathVariable int id) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(200).message("Work order retrieved successfully")
                .data(workOrderService.getById(id)).build();
    }

    @Operation(summary = "Create work order (repair / manufacture with planned materials)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<WorkOrderResponse> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(201).message("Work order created successfully")
                .data(workOrderService.create(request)).build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<WorkOrderResponse> confirm(@PathVariable int id) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(200).message("Work order confirmed")
                .data(workOrderService.confirm(id)).build();
    }

    @Operation(summary = "Consume materials (actual) — trừ kho + cộng qtyActual")
    @PostMapping("/{id}/consume")
    @PreAuthorize("hasAnyAuthority('INVENTORY_WRITE')")
    public ApiResponse<WorkOrderResponse> consume(@PathVariable int id,
                                                  @Valid @RequestBody ConsumeMaterialRequest request) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(200).message("Materials consumed successfully")
                .data(workOrderService.consume(id, request, securityService.getCurrentUsername())).build();
    }

    @PostMapping("/{id}/done")
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<WorkOrderResponse> done(@PathVariable int id) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(200).message("Work order done")
                .data(workOrderService.done(id)).build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE')")
    public ApiResponse<WorkOrderResponse> cancel(@PathVariable int id) {
        return ApiResponse.<WorkOrderResponse>builder()
                .statusCode(200).message("Work order cancelled")
                .data(workOrderService.cancel(id)).build();
    }

    @Operation(summary = "Upload ảnh nghiệm thu (JPG/PNG/WEBP, tối đa 10MB)")
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ORDER_WRITE', 'INVENTORY_WRITE')")
    public ApiResponse<WorkOrderAttachmentResponse> uploadAttachment(
            @PathVariable int id,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<WorkOrderAttachmentResponse>builder()
                .statusCode(201).message("Photo uploaded successfully")
                .data(workOrderService.uploadAttachment(id, file, securityService.getCurrentUsername())).build();
    }

    @Operation(summary = "List ảnh nghiệm thu")
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<WorkOrderAttachmentResponse>> attachments(@PathVariable int id) {
        return ApiResponse.<List<WorkOrderAttachmentResponse>>builder()
                .statusCode(200).message("Attachments retrieved successfully")
                .data(workOrderService.attachments(id)).build();
    }
}
