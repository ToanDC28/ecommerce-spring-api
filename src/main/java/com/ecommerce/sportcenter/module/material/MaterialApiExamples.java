package com.ecommerce.sportcenter.module.material;

public final class MaterialApiExamples {
    private MaterialApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Materials retrieved successfully",
             "data": {"content": [{"id": 1, "sku": "VT-THEP-CT3-10MM", "name": "Thép tấm CT3 10mm", "stockQty": 500}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String DETAIL_200 = """
            {"statusCode": 200, "message": "Material retrieved successfully",
             "data": {"id": 1, "sku": "VT-THEP-CT3-10MM", "name": "Thép tấm CT3 10mm"}}
            """;

    public static final String CREATE_201 = """
            {"statusCode": 201, "message": "Material created successfully",
             "data": {"id": 1, "sku": "VT-THEP-CT3-10MM", "name": "Thép tấm CT3 10mm"}}
            """;
}
