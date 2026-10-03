package com.ecommerce.sportcenter.module.base.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSearchRequest {
    @Schema(example = "name", description = "Sort by: name, code, group")
    private String sortBy;

    @Schema(example = "ASC", description = "Sort direction: ASC, DESC")
    private String sortDir;

    @Schema(example = "0", description = "Page number (0-based)")
    @Builder.Default
    private int page = 0;

    @Schema(example = "10", description = "Page size")
    @Builder.Default
    private int size = 10;

    public Sort toSort() {
        String property = isValidSortBy(sortBy) ? sortBy : "name";
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    protected boolean isValidSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return false;
        return sortBy.equalsIgnoreCase("name");
    }
}
