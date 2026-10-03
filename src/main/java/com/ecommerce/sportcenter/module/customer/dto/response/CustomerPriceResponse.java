package com.ecommerce.sportcenter.module.customer.dto.response;

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
public class CustomerPriceResponse {
    private int id;
    private int materialId;
    private String materialSku;
    private String materialName;
    private long sellPrice;
    private Long defaultSellPrice;
}
