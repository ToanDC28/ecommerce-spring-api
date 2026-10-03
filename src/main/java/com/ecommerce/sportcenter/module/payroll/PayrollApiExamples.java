package com.ecommerce.sportcenter.module.payroll;

public final class PayrollApiExamples {
    private PayrollApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Payrolls retrieved successfully",
             "data": {"content": [{"id": 1, "period": "2026-09", "status": "PENDING", "netPay": 8500000}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;
}
