package com.ecommerce.sportcenter.module.auth.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.module.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.sportcenter.module.auth.repository.RefreshTokenRepository;
import com.ecommerce.sportcenter.module.auth.service.RevocationService;
import com.ecommerce.sportcenter.module.base.security.JwtService;
import com.ecommerce.sportcenter.module.user.dto.mapper.UserMapper;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RevocationService revocationService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl service;

    private User user() {
        return User.builder().id(1).username("staff01").email("s@shop.local")
                .password("enc-old").enabled(true).roles(new HashSet<>()).build();
    }

    @Nested
    @DisplayName("changePassword():")
    class ChangePassword {

        @Test
        @DisplayName("success encodes new + revokes all sessions")
        void success() {
            var user = user();
            when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Old@123", "enc-old")).thenReturn(true);
            when(passwordEncoder.matches("New@123456", "enc-old")).thenReturn(false);
            when(passwordEncoder.encode("New@123456")).thenReturn("enc-new");

            service.changePassword("staff01", ChangePasswordRequest.builder()
                    .oldPassword("Old@123").newPassword("New@123456").build());

            verify(userRepository).save(user);
            verify(refreshTokenRepository).revokeAllByUserId(1);
        }

        @Test
        @DisplayName("reject wrong old password")
        void wrongOld() {
            var user = user();
            when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Wrong", "enc-old")).thenReturn(false);

            assertThatThrownBy(() -> service.changePassword("staff01", ChangePasswordRequest.builder()
                    .oldPassword("Wrong").newPassword("New@123456").build()))
                    .isInstanceOf(BadCredentialsException.class);
            verify(userRepository, never()).save(user);
        }

        @Test
        @DisplayName("reject same password")
        void samePassword() {
            var user = user();
            when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Old@123", "enc-old")).thenReturn(true);
            when(passwordEncoder.matches("Old@123", "enc-old")).thenReturn(true);

            assertThatThrownBy(() -> service.changePassword("staff01", ChangePasswordRequest.builder()
                    .oldPassword("Old@123").newPassword("Old@123").build()))
                    .isInstanceOf(BusinessValidationException.class);
        }
    }
}
