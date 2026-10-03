package com.ecommerce.sportcenter.module.customer;

public final class CustomerApiExamples {
    private CustomerApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Customers retrieved successfully",
             "data": {"content": [{"id": 1, "code": "KH-001", "name": "Anh Ba", "totalOwed": 1500000}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;
}
