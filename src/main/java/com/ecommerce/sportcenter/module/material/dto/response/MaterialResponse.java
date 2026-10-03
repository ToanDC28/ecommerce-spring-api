package com.ecommerce.sportcenter.module.material.dto.response;

import com.ecommerce.sportcenter.module.material.entity.MaterialUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {
    private int id;
    private Date createdDate;
    private Date updatedDate;
    private String sku;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private String brand;
    private MaterialUnit unit;
    private long costPrice;
    private Long sellPrice;
    private long stockQty;
    private long minStock;
    private String location;
    private boolean active;
    private boolean lowStock;
}
