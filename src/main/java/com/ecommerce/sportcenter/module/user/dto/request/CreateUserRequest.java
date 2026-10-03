package com.ecommerce.sportcenter.module.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private String fullName;

    /**
     * Internal app: ADMIN assigns one or more staff roles
     * (ADMIN, SALES_STAFF, WAREHOUSE_STAFF, ACCOUNTANT).
     */
    @NotEmpty
    private Set<String> roles;
}
