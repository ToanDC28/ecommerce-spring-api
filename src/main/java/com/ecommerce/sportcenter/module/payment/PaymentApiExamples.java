package com.ecommerce.sportcenter.module.payment;

public final class PaymentApiExamples {
    private PaymentApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Payments retrieved successfully",
             "data": {"content": [{"id": 1, "code": "PAY-2026-0001", "amount": 500000, "method": "CASH"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String CREATE_201 = """
            {"statusCode": 201, "message": "Payment recorded successfully",
             "data": {"id": 1, "code": "PAY-2026-0001", "amount": 500000, "method": "CASH"}}
            """;
}
