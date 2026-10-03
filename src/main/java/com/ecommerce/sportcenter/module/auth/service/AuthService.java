package com.ecommerce.sportcenter.module.auth.service;

import com.ecommerce.sportcenter.module.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.sportcenter.module.auth.dto.request.LoginRequest;
import com.ecommerce.sportcenter.module.auth.dto.request.RefreshRequest;
import com.ecommerce.sportcenter.module.auth.dto.response.AuthResponse;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshRequest request);
    void logout(String username, String accessToken, RefreshRequest request);
    void logoutAll(String username);
    UserResponse getCurrentUser(String username);
    void changePassword(String username, ChangePasswordRequest request);
}
