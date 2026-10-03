package com.ecommerce.sportcenter.module.base.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

/**
 * Base search/filter request with pagination. Per-module
 * {@code SearchXxxRequest} must extend this and override {@link #toSort()}
 * with a whitelist.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseFilterRequest {
    @Schema(example = "0", description = "Page number (0-based)")
    @lombok.Builder.Default
    private int page = 0;

    @Schema(example = "10", description = "Page size")
    @lombok.Builder.Default
    private int size = 10;

    @Schema(example = "name", description = "Sort property (whitelisted per module)")
    private String sortBy;

    @Schema(example = "ASC", description = "Sort direction: ASC, DESC")
    private String sortDir;

    @Schema(description = "Advanced keyword search across fields")
    private SearchRequest advancedSearch;

    @Schema(description = "Advanced structured filters")
    private java.util.List<FilterRequest> advancedFilter;

    public Sort toSort() {
        String property = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    public org.springframework.data.domain.Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.max(size, 1), toSort());
    }
}
