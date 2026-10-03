package com.ecommerce.sportcenter.module.user;

public final class UserApiExamples {
    private UserApiExamples() {
    }

    public static final String VIEW_200 = """
            {"statusCode": 200, "message": "Users retrieved successfully",
             "data": {"content": [{"id": 1, "username": "admin", "email": "admin@shop.local"}],
                      "totalElements": 1, "totalPages": 1, "size": 10, "number": 0, "first": true, "last": true}}
            """;

    public static final String DETAIL_200 = """
            {"statusCode": 200, "message": "User retrieved successfully",
             "data": {"id": 1, "username": "admin", "email": "admin@shop.local"}}
            """;

    public static final String CREATE_201 = """
            {"statusCode": 201, "message": "User created successfully",
             "data": {"id": 1, "username": "staff01", "email": "staff01@shop.local"}}
            """;
}
