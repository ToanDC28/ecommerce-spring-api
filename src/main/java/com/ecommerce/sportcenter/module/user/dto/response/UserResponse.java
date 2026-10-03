package com.ecommerce.sportcenter.module.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private int id;
    private Date createdDate;
    private Date updatedDate;
    private String username;
    private String email;
    private String fullName;
    private boolean enabled;
    private Set<String> roles;
}
