package com.ecommerce.sportcenter.module.base.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class FilterRequest {
    private String field;
    private String operator;
    private Object value;
    private String logic;
    private List<FilterRequest> filters;
}
