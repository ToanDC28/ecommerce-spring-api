package com.ecommerce.sportcenter.module.user.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.auth.repository.RefreshTokenRepository;
import com.ecommerce.sportcenter.module.role.entity.Role;
import com.ecommerce.sportcenter.module.role.repository.RoleRepository;
import com.ecommerce.sportcenter.module.user.dto.mapper.UserMapper;
import com.ecommerce.sportcenter.module.user.dto.request.AssignRolesRequest;
import com.ecommerce.sportcenter.module.user.dto.request.SearchUserRequest;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl service;

    private User admin;
    private User staff;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().name("ADMIN").build();
        Role salesRole = Role.builder().name("SALES_STAFF").build();
        admin = User.builder().id(1).username("admin").email("admin@shop.local")
                .password("enc").enabled(true).roles(new HashSet<>(Set.of(adminRole))).build();
        staff = User.builder().id(2).username("staff01").email("staff01@shop.local")
                .password("enc").enabled(true).roles(new HashSet<>(Set.of(salesRole))).build();
    }

    @Nested
    @DisplayName("search():")
    class Search {

        @Test
        @DisplayName("success with role filter returns page")
        void withRole() {
            var request = SearchUserRequest.builder().role("ADMIN").page(0).size(10).build();
            var response = UserResponse.builder().id(1).username("admin").build();
            var page = new PageImpl<>(List.of(admin));
            when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(userMapper.toResponse(admin)).thenReturn(response);

            var result = service.search(request);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("setEnabled():")
    class SetEnabled {

        @Test
        @DisplayName("reject self-disable")
        void selfDisable() {
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.setEnabled(1, false, "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("own account");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject disabling last enabled ADMIN")
        void lastAdmin() {
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            when(userRepository.countOtherEnabledAdmins(1)).thenReturn(0L);

            assertThatThrownBy(() -> service.setEnabled(1, false, "other-admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("last enabled ADMIN");
        }

        @Test
        @DisplayName("success disabling non-last admin revokes tokens")
        void success() {
            var response = UserResponse.builder().id(1).username("admin").enabled(false).build();
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            when(userRepository.countOtherEnabledAdmins(1)).thenReturn(1L);
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toResponse(any())).thenReturn(response);

            assertThat(service.setEnabled(1, false, "super-admin")).isEqualTo(response);
            verify(refreshTokenRepository).revokeAllByUserId(1);
        }
    }

    @Nested
    @DisplayName("delete():")
    class Delete {

        @Test
        @DisplayName("reject self-delete")
        void selfDelete() {
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.delete(1, "admin"))
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessageContaining("own account");
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("reject deleting last ADMIN")
        void lastAdmin() {
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            when(userRepository.countOtherEnabledAdmins(1)).thenReturn(0L);

            assertThatThrownBy(() -> service.delete(1, "other"))
                    .isInstanceOf(BusinessValidationException.class);
        }
    }

    @Nested
    @DisplayName("assignRoles():")
    class AssignRoles {

        @Test
        @DisplayName("reject demoting last ADMIN")
        void demoteLastAdmin() {
            Role sales = Role.builder().name("SALES_STAFF").build();
            when(userRepository.findById(1)).thenReturn(Optional.of(admin));
            when(roleRepository.findByName("SALES_STAFF")).thenReturn(Optional.of(sales));
            when(userRepository.countOtherEnabledAdmins(1)).thenReturn(0L);

            assertThatThrownBy(() -> service.assignRoles(1,
                    AssignRolesRequest.builder().roles(Set.of("SALES_STAFF")).build(), "other"))
                    .isInstanceOf(BusinessValidationException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("notFound when role missing")
        void roleMissing() {
            when(userRepository.findById(2)).thenReturn(Optional.of(staff));
            when(roleRepository.findByName("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignRoles(2,
                    AssignRolesRequest.builder().roles(Set.of("GHOST")).build(), "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword():")
    class ResetPassword {

        @Test
        @DisplayName("success encodes + revokes sessions")
        void success() {
            when(userRepository.findById(2)).thenReturn(Optional.of(staff));
            when(passwordEncoder.encode("New@123456")).thenReturn("enc-new");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toResponse(any())).thenReturn(UserResponse.builder().id(2).build());

            service.resetPassword(2, "New@123456");

            verify(passwordEncoder).encode("New@123456");
            verify(refreshTokenRepository).revokeAllByUserId(2);
        }

        @Test
        @DisplayName("reject short password")
        void shortPassword() {
            assertThatThrownBy(() -> service.resetPassword(2, "123"))
                    .isInstanceOf(BusinessValidationException.class);
            verify(userRepository, never()).save(any());
        }
    }
}
