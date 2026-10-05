package com.ecommerce.sportcenter.module.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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

    @Schema(example = "1", description = "Bậc lương theo HĐLĐ (optional, để trống khi chưa xếp)")
    private Integer salaryGradeId;

    @Schema(example = "8500000", description = "Lương cơ bản đã thỏa thuận (trống = theo grade)")
    @Min(value = 0, message = "Agreed base must be >= 0")
    private Long agreedBaseSalary;

    /**
     * Internal app: ADMIN assigns one or more staff roles
     * (ADMIN, SALES_STAFF, WAREHOUSE_STAFF, ACCOUNTANT).
     */
    @NotEmpty
    private Set<String> roles;
}
