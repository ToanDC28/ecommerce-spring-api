package com.ecommerce.sportcenter.module.inventory.dto.request;

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
public class SearchStockRequest extends BaseFilterRequest {

    @Schema(description = "Filter by warehouse id")
    private Integer warehouseId;

    @Schema(description = "Filter by material id")
    private Integer materialId;

    @Schema(example = "true", description = "Only stock <= material minStock")
    private Boolean lowStockOnly;

    private static final Set<String> ALLOWED = Set.of("id", "qtyOnHand", "createdDate");

    @Override
    public Sort toSort() {
        String property = getSortBy();
        if (property == null || !ALLOWED.contains(property)) {
            property = "id";
        }
        Sort.Direction direction = "DESC".equalsIgnoreCase(getSortDir())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
