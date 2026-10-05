package com.ecommerce.sportcenter.module.user.service;

import com.ecommerce.sportcenter.module.base.dto.response.PageResponse;
import com.ecommerce.sportcenter.module.user.dto.request.AssignRolesRequest;
import com.ecommerce.sportcenter.module.user.dto.request.CreateUserRequest;
import com.ecommerce.sportcenter.module.user.dto.request.SearchUserRequest;
import com.ecommerce.sportcenter.module.user.dto.request.UpdateContractRequest;
import com.ecommerce.sportcenter.module.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    PageResponse<UserResponse> search(SearchUserRequest request);

    Page<UserResponse> getAll(Pageable pageable);
    UserResponse getById(int id);
    UserResponse create(CreateUserRequest request);
    UserResponse assignRoles(int id, AssignRolesRequest request, String currentUsername);
    UserResponse setEnabled(int id, boolean enabled, String currentUsername);
    void delete(int id, String currentUsername);
    UserResponse resetPassword(int id, String newPassword);
    UserResponse updateContract(int id, UpdateContractRequest request);
    List<String> getAllRoleNames();
}
