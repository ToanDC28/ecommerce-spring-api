package com.ecommerce.sportcenter.module.inventory;

public final class InventoryApiExamples {
    private InventoryApiExamples() {
    }

    public static final String STOCK_VIEW_200 = """
            {"statusCode": 200, "message": "Stocks retrieved successfully",
             "data": {"content": [{"id": 1, "materialSku": "VT-THEP-CT3-10MM", "qtyOnHand": 500, "lowStock": false}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;
}
