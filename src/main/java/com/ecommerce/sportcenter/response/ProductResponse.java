package com.ecommerce.sportcenter.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private int id;
    private Date createdDate;
    private Date updatedDate;
    private String name;
    private String description;
    private long price;
    private String imageUrl;
    private BrandResponse brand;
    private TypeResponse type;
}
