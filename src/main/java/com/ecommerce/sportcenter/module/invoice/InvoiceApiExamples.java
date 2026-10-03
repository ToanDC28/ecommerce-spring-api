package com.ecommerce.sportcenter.module.invoice;

public final class InvoiceApiExamples {
    private InvoiceApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Invoices retrieved successfully",
             "data": {"content": [{"id": 1, "code": "INV-2026-00001", "type": "WORK", "grandTotal": 15000000}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String CREATE_201 = """
            {"statusCode": 201, "message": "Work invoice created successfully",
             "data": {"id": 1, "code": "INV-2026-00001", "type": "WORK", "grandTotal": 15000000}}
            """;
}
