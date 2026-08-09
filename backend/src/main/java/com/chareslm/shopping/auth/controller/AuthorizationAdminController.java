package com.chareslm.shopping.auth.controller;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.service.AuthorizationManagementService;
import com.chareslm.shopping.auth.service.AuthorizationQueryService;
import com.chareslm.shopping.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/authorization")
public class AuthorizationAdminController {
    private final AuthorizationQueryService authorizationQueryService;
    private final AuthorizationManagementService authorizationManagementService;

    public AuthorizationAdminController(AuthorizationQueryService authorizationQueryService,
                                        AuthorizationManagementService authorizationManagementService) {
        this.authorizationQueryService = authorizationQueryService;
        this.authorizationManagementService = authorizationManagementService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success(authorizationQueryService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success(authorizationQueryService.listPermissions());
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('system:user:role:assign')")
    public ApiResponse<Void> replaceUserRoles(@PathVariable Long userId,
                                              @jakarta.validation.Valid @RequestBody AssignUserRolesRequest request) {
        authorizationManagementService.replaceUserRoles(
                com.chareslm.shopping.security.context.CurrentUser.require().userId(), userId, request);
        return ApiResponse.success(null);
    }
}
