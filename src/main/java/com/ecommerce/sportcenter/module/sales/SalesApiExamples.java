package com.ecommerce.sportcenter.module.sales;

public final class SalesApiExamples {
    private SalesApiExamples() {
    }

    public static final String SO_VIEW_200 = """
            {"statusCode": 200, "message": "Sales orders retrieved successfully",
             "data": {"content": [{"id": 1, "code": "SO-2026-0001", "status": "CONFIRMED", "grandTotal": 150000}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String GIN_VIEW_200 = """
            {"statusCode": 200, "message": "Goods issues retrieved successfully",
             "data": {"content": [{"id": 1, "code": "GIN-2026-0001", "status": "CONFIRMED"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;
}
