package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;

import java.util.List;

public interface AuthorizationQueryService {
    List<RoleResponse> listRoles();

    List<PermissionResponse> listPermissions();
}
