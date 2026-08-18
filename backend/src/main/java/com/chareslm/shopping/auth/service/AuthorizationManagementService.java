package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.dto.request.CreateAdminUserRequest;
import com.chareslm.shopping.auth.dto.response.CreatedAdminUserResponse;

public interface AuthorizationManagementService {
    void replaceUserRoles(Long operatorUserId, Long targetUserId, AssignUserRolesRequest request);

    CreatedAdminUserResponse createUser(Long operatorUserId, CreateAdminUserRequest request);

    CreatedAdminUserResponse retryCredentialEmail(Long operatorUserId, Long targetUserId);
}
