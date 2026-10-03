package com.ecommerce.sportcenter.module.workorder.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderStatus;
import com.ecommerce.sportcenter.module.workorder.entity.WorkOrderType;
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
public class SearchWorkOrderRequest extends BaseFilterRequest {

    @Schema(description = "Keyword in code, customer, contract")
    private String keyword;

    @Schema(description = "Filter by type")
    private WorkOrderType type;

    @Schema(description = "Filter by status")
    private WorkOrderStatus status;

    private static final Set<String> ALLOWED = Set.of("id", "code", "createdDate", "dueDate");

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
