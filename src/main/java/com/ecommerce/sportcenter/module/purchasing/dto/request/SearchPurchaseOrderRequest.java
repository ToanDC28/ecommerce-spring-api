package com.ecommerce.sportcenter.module.purchasing.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.purchasing.entity.PurchaseOrderStatus;
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
public class SearchPurchaseOrderRequest extends BaseFilterRequest {

    @Schema(description = "Keyword in code, note")
    private String keyword;

    @Schema(description = "Filter by supplier id")
    private Integer supplierId;

    @Schema(description = "Filter by status")
    private PurchaseOrderStatus status;

    private static final Set<String> ALLOWED = Set.of("id", "code", "orderDate", "createdDate");

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
