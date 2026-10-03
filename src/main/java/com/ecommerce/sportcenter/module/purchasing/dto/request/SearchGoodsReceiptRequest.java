package com.ecommerce.sportcenter.module.purchasing.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.purchasing.entity.GoodsReceiptStatus;
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
public class SearchGoodsReceiptRequest extends BaseFilterRequest {

    @Schema(description = "Keyword in code")
    private String keyword;

    @Schema(description = "Filter by status")
    private GoodsReceiptStatus status;

    @Schema(description = "Filter by purchase order id")
    private Integer purchaseOrderId;

    @Schema(description = "Filter by warehouse id")
    private Integer warehouseId;

    private static final Set<String> ALLOWED = Set.of("id", "code", "receiptDate", "createdDate");

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
