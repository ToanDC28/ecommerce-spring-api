package com.ecommerce.sportcenter.module.workorder;

public final class WorkOrderApiExamples {
    private WorkOrderApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Work orders retrieved successfully",
             "data": {"content": [{"id": 1, "code": "WO-2026-0001", "type": "REPAIR", "status": "IN_PROGRESS"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String DETAIL_200 = """
            {"statusCode": 200, "message": "Work order retrieved successfully",
             "data": {"id": 1, "code": "WO-2026-0001", "materials": [{"materialSku": "VT-THEP-CT3-10MM", "qtyPlanned": 10, "qtyActual": 8}]}}
            """;
}
