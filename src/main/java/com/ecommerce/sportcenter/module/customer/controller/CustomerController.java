package com.ecommerce.sportcenter.module.customer.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.customer.CustomerApiExamples;
import com.ecommerce.sportcenter.module.customer.dto.request.CreateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SearchCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.SetCustomerPriceRequest;
import com.ecommerce.sportcenter.module.customer.dto.request.UpdateCustomerRequest;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerPriceResponse;
import com.ecommerce.sportcenter.module.customer.dto.response.CustomerResponse;
import com.ecommerce.sportcenter.module.customer.service.CustomerService;
import com.ecommerce.sportcenter.module.invoice.dto.response.InvoiceResponse;
import com.ecommerce.sportcenter.module.workorder.dto.response.WorkOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('CUSTOMER_READ')")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Search customers (paginated, autocomplete theo tên/SĐT khi tạo đơn)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customers retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "CustomerList", value = CustomerApiExamples.VIEW_200)))
    })
    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> search(@ParameterObject SearchCustomerRequest request) {
        return ApiResponse.<PageResponse<CustomerResponse>>builder()
                .statusCode(200).message("Customers retrieved successfully")
                .data(customerService.search(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getById(@PathVariable int id) {
        return ApiResponse.<CustomerResponse>builder()
                .statusCode(200).message("Customer retrieved successfully")
                .data(customerService.getById(id)).build();
    }

    @Operation(summary = "Create customer (chọn nhanh khi tạo đơn nếu chưa có)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER_WRITE')")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.<CustomerResponse>builder()
                .statusCode(201).message("Customer created successfully")
                .data(customerService.create(request)).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_WRITE')")
    public ApiResponse<CustomerResponse> update(@PathVariable int id,
                                                @Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.<CustomerResponse>builder()
                .statusCode(200).message("Customer updated successfully")
                .data(customerService.update(id, request)).build();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_WRITE')")
    public ApiResponse<CustomerResponse> setActive(@PathVariable int id,
                                                   @RequestBody Map<String, Boolean> body) {
        return ApiResponse.<CustomerResponse>builder()
                .statusCode(200).message("Customer status updated successfully")
                .data(customerService.setActive(id, Boolean.TRUE.equals(body.get("active")))).build();
    }

    @Operation(summary = "Open invoices (công nợ chưa trả) của khách")
    @GetMapping("/{id}/debts")
    public ApiResponse<List<InvoiceResponse>> debts(@PathVariable int id) {
        return ApiResponse.<List<InvoiceResponse>>builder()
                .statusCode(200).message("Customer debts retrieved successfully")
                .data(customerService.debts(id)).build();
    }

    @Operation(summary = "Lịch sử work orders của khách")
    @GetMapping("/{id}/work-orders")
    public ApiResponse<List<WorkOrderResponse>> workOrders(@PathVariable int id) {
        return ApiResponse.<List<WorkOrderResponse>>builder()
                .statusCode(200).message("Customer work orders retrieved successfully")
                .data(customerService.workOrders(id)).build();
    }

    @Operation(summary = "Bảng giá riêng của khách")
    @GetMapping("/{id}/prices")
    public ApiResponse<List<CustomerPriceResponse>> prices(@PathVariable int id) {
        return ApiResponse.<List<CustomerPriceResponse>>builder()
                .statusCode(200).message("Customer prices retrieved successfully")
                .data(customerService.prices(id)).build();
    }

    @Operation(summary = "Đặt giá bán riêng cho khách (giá sỉ thợ...)")
    @PostMapping("/{id}/prices")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_WRITE')")
    public ApiResponse<CustomerPriceResponse> setPrice(@PathVariable int id,
                                                       @Valid @RequestBody SetCustomerPriceRequest request) {
        return ApiResponse.<CustomerPriceResponse>builder()
                .statusCode(200).message("Customer price set successfully")
                .data(customerService.setPrice(id, request)).build();
    }

    @Operation(summary = "Xóa giá riêng (về giá chung)")
    @DeleteMapping("/{id}/prices/{materialId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER_WRITE')")
    public ApiResponse<Void> deletePrice(@PathVariable int id, @PathVariable int materialId) {
        customerService.deletePrice(id, materialId);
        return ApiResponse.<Void>builder()
                .statusCode(200).message("Customer price deleted successfully").build();
    }
}
