package com.ecommerce.sportcenter.module.auth;

public final class AuthApiExamples {
    private AuthApiExamples() {
    }

    public static final String LOGIN_200 = """
            {"statusCode": 200, "message": "Login successful",
             "data": {"accessToken": "eyJ...", "refreshToken": "eyJ..."}}
            """;
}
