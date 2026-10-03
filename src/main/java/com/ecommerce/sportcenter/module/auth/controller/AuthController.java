package com.ecommerce.sportcenter.module.auth.controller;

import com.ecommerce.sportcenter.module.auth.AuthApiExamples;
import com.ecommerce.sportcenter.module.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.sportcenter.module.auth.dto.request.LoginRequest;
import com.ecommerce.sportcenter.module.auth.dto.request.RefreshRequest;
import com.ecommerce.sportcenter.module.auth.dto.response.AuthResponse;
import com.ecommerce.sportcenter.module.base.dto.response.ApiResponse;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import com.ecommerce.sportcenter.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with username/password")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "Login", value = AuthApiExamples.LOGIN_200)))
    })
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .statusCode(200)
                .message("Login successful")
                .data(authService.login(request))
                .build();
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .statusCode(200)
                .message("Token refreshed successfully")
                .data(authService.refresh(request))
                .build();
    }

    @Operation(summary = "Logout current session")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserDetails principal,
                                    HttpServletRequest httpRequest,
                                    @RequestBody(required = false) RefreshRequest request) {
        authService.logout(principal.getUsername(), extractBearer(httpRequest), request);
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Logged out successfully")
                .build();
    }

    @Operation(summary = "Logout all sessions")
    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(@AuthenticationPrincipal UserDetails principal) {
        authService.logoutAll(principal.getUsername());
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Logged out from all devices")
                .build();
    }

    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.<UserResponse>builder()
                .statusCode(200)
                .message("Current user retrieved successfully")
                .data(authService.getCurrentUser(principal.getUsername()))
                .build();
    }

    @Operation(summary = "Change own password (revokes all sessions)")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Password changed successfully, please login again")
                .build();
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
