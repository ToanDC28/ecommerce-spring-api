package com.ecommerce.sportcenter.module.base.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequest {
    private String keyword;
    private List<String> fields;
}