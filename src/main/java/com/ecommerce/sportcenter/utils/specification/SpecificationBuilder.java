package com.ecommerce.sportcenter.utils.specification;
import com.ecommerce.sportcenter.module.base.dto.request.BaseFilterRequest;
import com.ecommerce.sportcenter.module.base.dto.request.FilterRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class SpecificationBuilder<T> {

    public Specification<T> build(BaseFilterRequest request) {
        Specification<T> spec = null;

        if (request.getAdvancedFilter() != null && !request.getAdvancedFilter().isEmpty()) {
            for (FilterRequest filter : request.getAdvancedFilter()) {
                Specification<T> fieldSpec = new DynamicSpecification<>(filter);
                spec = (spec == null) ? Specification.where(fieldSpec) : spec.and(fieldSpec);
            }
        }

        if (request.getAdvancedSearch() != null && request.getAdvancedSearch().getKeyword() != null) {
            String keyword = request.getAdvancedSearch().getKeyword();
            List<String> fields = request.getAdvancedSearch().getFields();

            if (fields != null && !fields.isEmpty()) {
                Specification<T> searchSpec = null;

                for (String field : fields) {
                    FilterRequest searchFilter = new FilterRequest();
                    searchFilter.setField(field);
                    searchFilter.setOperator("contains");
                    searchFilter.setValue(keyword);

                    Specification<T> fieldSpec = new DynamicSpecification<>(searchFilter);
                    searchSpec = (searchSpec == null) ? Specification.where(fieldSpec) : searchSpec.or(fieldSpec);
                }

                spec = (spec == null) ? Specification.where(searchSpec) : spec.and(searchSpec);
            }
        }

        return spec == null ? (root, query, builder) -> builder.conjunction() : spec;
    }
}
