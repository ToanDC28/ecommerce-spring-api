package com.ecommerce.sportcenter.request;

import lombok.Data;

@Data
public class CreateProductRequest {
    private String name;
    private String description;
    private long price;
    private String imageUrl;
    private int brandId;
    private int typeId;
}
