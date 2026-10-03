package com.ecommerce.sportcenter.module.material.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.material.MaterialApiExamples;
import com.ecommerce.sportcenter.module.material.dto.request.CreateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.SearchMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.request.UpdateMaterialRequest;
import com.ecommerce.sportcenter.module.material.dto.response.MaterialResponse;
import com.ecommerce.sportcenter.module.material.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('PRODUCT_READ')")
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "Search and list materials (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Materials retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "MaterialList", value = MaterialApiExamples.VIEW_200))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_READ')")
    public ApiResponse<PageResponse<MaterialResponse>> search(@ParameterObject SearchMaterialRequest request) {
        return ApiResponse.<PageResponse<MaterialResponse>>builder()
                .statusCode(200)
                .message("Materials retrieved successfully")
                .data(materialService.search(request))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_READ')")
    public ApiResponse<MaterialResponse> getById(@PathVariable int id) {
        return ApiResponse.<MaterialResponse>builder()
                .statusCode(200)
                .message("Material retrieved successfully")
                .data(materialService.getById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'PRODUCT_CREATE')")
    public ApiResponse<MaterialResponse> create(@Valid @RequestBody CreateMaterialRequest request) {
        return ApiResponse.<MaterialResponse>builder()
                .statusCode(201)
                .message("Material created successfully")
                .data(materialService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'PRODUCT_UPDATE')")
    public ApiResponse<MaterialResponse> update(@PathVariable int id,
                                                @Valid @RequestBody UpdateMaterialRequest request) {
        return ApiResponse.<MaterialResponse>builder()
                .statusCode(200)
                .message("Material updated successfully")
                .data(materialService.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE', 'PRODUCT_UPDATE')")
    public ApiResponse<MaterialResponse> setActive(@PathVariable int id,
                                                   @RequestBody Map<String, Boolean> body) {
        return ApiResponse.<MaterialResponse>builder()
                .statusCode(200)
                .message("Material status updated successfully")
                .data(materialService.setActive(id, Boolean.TRUE.equals(body.get("active"))))
                .build();
    }
}
