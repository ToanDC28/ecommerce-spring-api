package com.ecommerce.sportcenter.module.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockValueResponse {
    private long totalValue; // sum(stockQty * costPrice)
    private long materialCount;
    private List<LowStockRow> lowStock; // stockQty <= minStock

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockRow {
        private int materialId;
        private String sku;
        private String name;
        private long stockQty;
        private long minStock;
    }
}
