package com.ecommerce.sportcenter.module.purchasing;

public final class PurchasingApiExamples {
    private PurchasingApiExamples() {
    }

    public static final String PO_VIEW_200 = """
            {"statusCode": 200, "message": "Purchase orders retrieved successfully",
             "data": {"content": [{"id": 1, "code": "PO-2026-0001", "status": "SENT", "totalAmount": 2500000}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String GRN_VIEW_200 = """
            {"statusCode": 200, "message": "Goods receipts retrieved successfully",
             "data": {"content": [{"id": 1, "code": "GRN-2026-0001", "status": "CONFIRMED"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;
}
