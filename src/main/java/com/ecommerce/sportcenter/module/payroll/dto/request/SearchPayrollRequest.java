package com.ecommerce.sportcenter.module.payroll.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.payroll.entity.PayrollStatus;
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
public class SearchPayrollRequest extends BaseFilterRequest {

    @Schema(example = "2026-09", description = "Filter by period YYYY-MM")
    private String period;

    @Schema(example = "2", description = "Filter by staff id")
    private Integer staffId;

    @Schema(description = "Filter by status")
    private PayrollStatus status;

    private static final Set<String> ALLOWED = Set.of("id", "period", "netPay", "createdDate");

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
