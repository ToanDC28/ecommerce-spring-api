package com.ecommerce.sportcenter.module.inventory.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.inventory.entity.StockRefType;
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
public class SearchStockTransactionRequest extends BaseFilterRequest {

    @Schema(description = "Filter by material id")
    private Integer materialId;

    @Schema(description = "Filter by warehouse id")
    private Integer warehouseId;

    @Schema(description = "Filter by ref type (GRN, GIN, WORK_ORDER)")
    private StockRefType refType;

    private static final Set<String> ALLOWED = Set.of("id", "createdDate");

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
