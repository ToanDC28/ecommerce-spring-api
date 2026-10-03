package com.ecommerce.sportcenter.module.user.service.impl;

import com.ecommerce.sportcenter.exception.BusinessValidationException;
import com.ecommerce.sportcenter.exception.ResourceNotFoundException;
import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.role.entity.Role;
import com.ecommerce.sportcenter.module.user.dto.mapper.UserMapper;
import com.ecommerce.sportcenter.module.user.dto.request.SearchUserRequest;
import com.ecommerce.sportcenter.module.user.entity.User;
import com.ecommerce.sportcenter.module.auth.repository.RefreshTokenRepository;
import com.ecommerce.sportcenter.module.role.repository.RoleRepository;
import com.ecommerce.sportcenter.module.user.repository.UserRepository;
import com.ecommerce.sportcenter.module.user.dto.request.AssignRolesRequest;
import com.ecommerce.sportcenter.module.user.dto.request.CreateUserRequest;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.ecommerce.sportcenter.module.user.service.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(SearchUserRequest request) {
        log.info("Search users - keyword={}, role={}, enabled={}, page={}, size={}",
                request.getKeyword(), request.getRole(), request.getEnabled(), request.getPage(), request.getSize());
        var pageable = PageRequest.of(request.getPage(), request.getSize(), request.toSort());
        Specification<User> spec = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String like = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("username")), like),
                        builder.like(builder.lower(root.get("email")), like),
                        builder.like(builder.lower(root.get("fullName")), like)));
            }
            if (request.getEnabled() != null) {
                predicates.add(builder.equal(root.get("enabled"), request.getEnabled()));
            }
            if (request.getRole() != null && !request.getRole().isBlank()) {
                var rolesJoin = root.join("roles");
                predicates.add(builder.equal(rolesJoin.get("name"), request.getRole().trim()));
                // join on a collection may duplicate rows when paginating
                query.distinct(true);
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var page = userRepository.findAll(spec, pageable);
        var content = page.getContent().stream().map(userMapper::toResponse).toList();
        return PageResponse.<UserResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(int id) {
        log.info("Get user by id - id={}", id);
        return userMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessValidationException("Username '" + request.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email '" + request.getEmail() + "' already exists");
        }
        Set<Role> roles = request.getRoles().stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name)))
                .collect(Collectors.toSet());
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .enabled(true)
                .roles(new HashSet<>(roles))
                .build();
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse assignRoles(int id, AssignRolesRequest request, String currentUsername) {
        User user = findOrThrow(id);
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));
            roles.add(role);
        }
        boolean keepsAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        if (isEnabledAdmin(user) && !keepsAdmin) {
            guardLastAdmin(user.getId());
        }
        user.setRoles(roles);
        log.info("Roles assigned - userId={}, roles={}, by={}", id, request.getRoles(), currentUsername);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse setEnabled(int id, boolean enabled, String currentUsername) {
        User user = findOrThrow(id);
        if (!enabled) {
            if (currentUsername != null && currentUsername.equals(user.getUsername())) {
                throw new BusinessValidationException("You cannot disable your own account");
            }
            if (isEnabledAdmin(user)) {
                guardLastAdmin(user.getId());
            }
        }
        user.setEnabled(enabled);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        if (!enabled) {
            // Disabled users lose all sessions immediately (refresh side;
            // access tokens expire within minutes and are denylisted on logout).
            refreshTokenRepository.revokeAllByUserId(id);
        }
        log.info("User enabled flag changed - userId={}, enabled={}, by={}", id, enabled, currentUsername);
        return response;
    }

    @Override
    @Transactional
    public void delete(int id, String currentUsername) {
        User user = findOrThrow(id);
        if (currentUsername != null && currentUsername.equals(user.getUsername())) {
            throw new BusinessValidationException("You cannot delete your own account");
        }
        if (isEnabledAdmin(user)) {
            guardLastAdmin(user.getId());
        }
        log.info("User deleted - userId={}, by={}", id, currentUsername);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse resetPassword(int id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessValidationException("New password must be at least 6 characters");
        }
        User user = findOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Force re-login on all devices after an admin reset.
        refreshTokenRepository.revokeAllByUserId(id);
        log.info("Password reset by admin - userId={}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllRoleNames() {
        return roleRepository.findAll().stream().map(Role::getName).toList();
    }

    private User findOrThrow(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private boolean isEnabledAdmin(User user) {
        return user.isEnabled() && user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
    }

    private void guardLastAdmin(int excludeId) {
        if (userRepository.countOtherEnabledAdmins(excludeId) == 0) {
            throw new BusinessValidationException("Cannot remove/disable the last enabled ADMIN");
        }
    }
}
