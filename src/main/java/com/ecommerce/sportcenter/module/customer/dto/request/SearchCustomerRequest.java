package com.ecommerce.sportcenter.module.customer.dto.request;

import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.customer.entity.CustomerType;
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
public class SearchCustomerRequest extends BaseFilterRequest {

    @Schema(example = "ba", description = "Keyword in code, name, phone")
    private String keyword;

    @Schema(description = "Filter by type")
    private CustomerType type;

    @Schema(description = "Filter by active")
    private Boolean active;

    private static final Set<String> ALLOWED = Set.of("id", "code", "name", "createdDate");

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
