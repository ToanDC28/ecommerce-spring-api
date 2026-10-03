package com.ecommerce.sportcenter.module.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRolesRequest {
    @NotEmpty
    private Set<String> roles;
}
