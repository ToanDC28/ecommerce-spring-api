package com.ecommerce.sportcenter.module.user.controller;

import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.base.security.service.SecurityService;
import com.ecommerce.sportcenter.module.user.UserApiExamples;
import com.ecommerce.sportcenter.module.user.dto.request.AssignRolesRequest;
import com.ecommerce.sportcenter.module.user.dto.request.CreateUserRequest;
import com.ecommerce.sportcenter.module.user.dto.request.ResetPasswordRequest;
import com.ecommerce.sportcenter.module.user.dto.request.SearchUserRequest;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import com.ecommerce.sportcenter.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('USER_READ')")
public class UserController {

    private final UserService userService;
    private final SecurityService securityService;

    @Operation(summary = "Search and list users (paginated)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "UserList", value = UserApiExamples.VIEW_200))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_READ')")
    public ApiResponse<PageResponse<UserResponse>> search(@ParameterObject SearchUserRequest request) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .statusCode(200)
                .message("Users retrieved successfully")
                .data(userService.search(request))
                .build();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('USER_READ')")
    public ApiResponse<List<String>> getAllRoles() {
        return ApiResponse.<List<String>>builder()
                .statusCode(200)
                .message("Roles retrieved successfully")
                .data(userService.getAllRoleNames())
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER_READ')")
    public ApiResponse<UserResponse> getById(@PathVariable int id) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(200)
                .message("User retrieved successfully")
                .data(userService.getById(id))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER_WRITE', 'USER_CREATE')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(201)
                .message("User created successfully")
                .data(userService.create(request))
                .build();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyAuthority('USER_WRITE', 'USER_UPDATE')")
    public ApiResponse<UserResponse> assignRoles(@PathVariable int id,
                                                 @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(200)
                .message("Roles assigned successfully")
                .data(userService.assignRoles(id, request, securityService.getCurrentUsername()))
                .build();
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAnyAuthority('USER_WRITE', 'USER_UPDATE')")
    public ApiResponse<UserResponse> setEnabled(@PathVariable int id,
                                                @RequestBody Map<String, Boolean> body) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(200)
                .message("User status updated successfully")
                .data(userService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")),
                        securityService.getCurrentUsername()))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER_WRITE', 'USER_DELETE')")
    public ApiResponse<Void> delete(@PathVariable int id) {
        userService.delete(id, securityService.getCurrentUsername());
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("User deleted successfully")
                .build();
    }

    @Operation(summary = "Admin reset user password (forces re-login)")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyAuthority('USER_WRITE', 'USER_UPDATE')")
    public ApiResponse<UserResponse> resetPassword(@PathVariable int id,
                                                   @Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(200)
                .message("Password reset successfully")
                .data(userService.resetPassword(id, request.getNewPassword()))
                .build();
    }
}
