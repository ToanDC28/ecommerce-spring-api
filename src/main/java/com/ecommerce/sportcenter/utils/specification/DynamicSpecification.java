package com.ecommerce.sportcenter.utils.specification;

import com.ecommerce.sportcenter.module.base.dto.request.FilterRequest;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DynamicSpecification<T> implements Specification<T> {

    private final FilterRequest criteria;

    public DynamicSpecification(FilterRequest criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        if (criteria.getLogic() != null && criteria.getFilters() != null && !criteria.getFilters().isEmpty()) {
            Predicate[] predicates = criteria.getFilters().stream()
                    .map(filter -> new DynamicSpecification<T>(filter).toPredicate(root, query, builder))
                    .toArray(Predicate[]::new);

            return criteria.getLogic().equalsIgnoreCase("OR")
                    ? builder.or(predicates)
                    : builder.and(predicates);
        }

        Path<Object> path = getPath(root, criteria.getField());
        Object castedValue = castToRequiredType(path.getJavaType(), criteria.getValue().toString());

        return switch (criteria.getOperator().toLowerCase()) {
            case "eq" -> builder.equal(path, castedValue);
            case "neq" -> builder.notEqual(path, castedValue);
            case "gt" -> builder.greaterThan(path.as(Comparable.class), (Comparable) castedValue);
            case "lt" -> builder.lessThan(path.as(Comparable.class), (Comparable) castedValue);
            case "gte" -> builder.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) castedValue);
            case "lte" -> builder.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) castedValue);
            case "contains" -> builder.like(builder.lower(path.as(String.class)), "%" + castedValue.toString().toLowerCase() + "%");
            case "startswith" -> builder.like(builder.lower(path.as(String.class)), castedValue.toString().toLowerCase() + "%");
            case "endswith" -> builder.like(builder.lower(path.as(String.class)), "%" + castedValue.toString().toLowerCase());
            default -> throw new IllegalArgumentException("Operator không hợp lệ: " + criteria.getOperator());
        };
    }

    private Path<Object> getPath(Root<T> root, String attributeName) {
        Path<?> path = root;
        for (String part : attributeName.split("\\.")) {
            path = path.get(part);
        }
        return (Path<Object>) path;
    }

    private Object castToRequiredType(Class<?> fieldType, String value) {
        if (fieldType.isAssignableFrom(Double.class)) return Double.valueOf(value);
        if (fieldType.isAssignableFrom(Integer.class)) return Integer.valueOf(value);
        if (fieldType.isAssignableFrom(Long.class)) return Long.valueOf(value);
        if (fieldType.isAssignableFrom(Boolean.class)) return Boolean.valueOf(value);
        if (Enum.class.isAssignableFrom(fieldType)) return Enum.valueOf((Class<Enum>) fieldType, value);
        if (fieldType.isAssignableFrom(LocalDate.class)) return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        if (fieldType.isAssignableFrom(LocalDateTime.class)) return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return value;
    }
}