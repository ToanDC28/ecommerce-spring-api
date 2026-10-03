package com.ecommerce.sportcenter.module.user.dto.mapper;

import com.ecommerce.sportcenter.module.role.entity.Role;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import com.ecommerce.sportcenter.module.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getUpdatedDate())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(user.getRoles() == null ? Set.of()
                        : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }
}
