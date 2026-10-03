package com.ecommerce.sportcenter.module.supplier;

public final class SupplierApiExamples {
    private SupplierApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Suppliers retrieved successfully",
             "data": {"content": [{"id": 1, "code": "SUP-001", "name": "ACME Parts"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String DETAIL_200 = """
            {"statusCode": 200, "message": "Supplier retrieved successfully",
             "data": {"id": 1, "code": "SUP-001", "name": "ACME Parts"}}
            """;

    public static final String CREATE_201 = """
            {"statusCode": 201, "message": "Supplier created successfully",
             "data": {"id": 1, "code": "SUP-001", "name": "ACME Parts"}}
            """;
}
