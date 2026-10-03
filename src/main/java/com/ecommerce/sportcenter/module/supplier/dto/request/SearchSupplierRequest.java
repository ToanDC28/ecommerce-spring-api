package com.ecommerce.sportcenter.module.supplier.dto.request;

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
public class SearchSupplierRequest extends BaseFilterRequest {

    @Schema(example = "SUP", description = "Keyword in code, name, phone")
    private String keyword;

    @Schema(example = "true", description = "Filter by active flag")
    private Boolean isActive;

    private static final Set<String> ALLOWED = Set.of("id", "code", "name", "createdDate");

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
