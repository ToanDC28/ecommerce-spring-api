package com.ecommerce.sportcenter.module.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private int id;
    private String code;
    private String name;
    private String address;
    private boolean active;
}
