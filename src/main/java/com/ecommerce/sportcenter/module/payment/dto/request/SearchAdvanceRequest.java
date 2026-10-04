package com.ecommerce.sportcenter.module.payment.dto.request;

import com.ecommerce.sportcenter.module.payment.entity.AdvanceStatus;
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
public class SearchAdvanceRequest extends com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest {

    @Schema(description = "Filter by customer id")
    private Integer customerId;

    @Schema(description = "Filter by work order id")
    private Integer workOrderId;

    @Schema(description = "Filter by sales order id")
    private Integer salesOrderId;

    @Schema(description = "Filter by status")
    private AdvanceStatus status;

    private static final Set<String> ALLOWED = Set.of("id", "code", "amount", "createdDate");

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
