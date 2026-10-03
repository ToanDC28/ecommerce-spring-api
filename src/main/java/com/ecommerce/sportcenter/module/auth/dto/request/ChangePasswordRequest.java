package com.ecommerce.sportcenter.module.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    @Schema(example = "Old@123", description = "Current password")
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @Schema(example = "New@123456", description = "New password (min 6 chars)")
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "New password must be 6-100 characters")
    private String newPassword;
}
