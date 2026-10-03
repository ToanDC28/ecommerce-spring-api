package com.ecommerce.sportcenter.module.user.dto.request;

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
public class SearchUserRequest extends BaseFilterRequest {

    @Schema(example = "admin", description = "Keyword in username, email, fullName")
    private String keyword;

    @Schema(example = "ADMIN", description = "Filter by role name (ADMIN, SALES_STAFF, WAREHOUSE_STAFF, ACCOUNTANT)")
    private String role;

    @Schema(example = "true", description = "Filter by enabled flag")
    private Boolean enabled;

    private static final Set<String> ALLOWED = Set.of("id", "username", "email", "createdDate");

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
