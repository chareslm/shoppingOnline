package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
import com.chareslm.shopping.auth.dto.response.AdminUserResponse;
import com.chareslm.shopping.common.api.PageResponse;

import java.util.List;

public interface AuthorizationQueryService {
    List<RoleResponse> listRoles();

    List<PermissionResponse> listPermissions();

    PageResponse<AdminUserResponse> listUsers(String keyword, String status, int page, int pageSize);
}
