package com.ecommerce.sportcenter.module.material.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SearchMaterialRequest extends BaseFilterRequest {

    @Schema(example = "thép", description = "Keyword in sku, name")
    private String keyword;

    @Schema(example = "VT-THEP", description = "Filter by SKU prefix")
    private String sku;

    @Schema(example = "1", description = "Filter by category id")
    private Integer categoryId;

    @Schema(example = "true", description = "Low stock only (stockQty <= minStock)")
    private Boolean lowStockOnly;

    @Schema(example = "true", description = "Filter by active")
    private Boolean active;

    private static final Set<String> ALLOWED = Set.of("id", "sku", "name", "stockQty", "createdDate");

    @Override
    public Sort toSort() {
        String property = getSortBy();
        if (property == null || !ALLOWED.contains(property)) {
            property = "name";
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(getSortDir())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
