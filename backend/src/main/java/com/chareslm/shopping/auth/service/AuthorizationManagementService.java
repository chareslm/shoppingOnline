package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;

public interface AuthorizationManagementService {
    void replaceUserRoles(Long operatorUserId, Long targetUserId, AssignUserRolesRequest request);
}
