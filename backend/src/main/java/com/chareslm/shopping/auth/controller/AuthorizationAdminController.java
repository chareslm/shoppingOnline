package com.chareslm.shopping.auth.controller;

import com.chareslm.shopping.auth.dto.response.PermissionResponse;
import com.chareslm.shopping.auth.dto.response.RoleResponse;
import com.chareslm.shopping.auth.dto.response.AdminUserResponse;
import com.chareslm.shopping.auth.dto.request.AssignUserRolesRequest;
import com.chareslm.shopping.auth.dto.request.CreateAdminUserRequest;
import com.chareslm.shopping.auth.dto.response.CreatedAdminUserResponse;
import com.chareslm.shopping.auth.service.AuthorizationManagementService;
import com.chareslm.shopping.auth.service.AuthorizationQueryService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/authorization")
@Validated
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

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(required = false) @Size(max = 128) String keyword,
            @RequestParam(required = false) @Pattern(regexp = "ACTIVE|DISABLED|LOCKED|PENDING_VERIFICATION") String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.success(authorizationQueryService.listUsers(keyword, status, page, pageSize));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('system:user:create')")
    public ApiResponse<CreatedAdminUserResponse> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return ApiResponse.success(authorizationManagementService.createUser(
                com.chareslm.shopping.security.context.CurrentUser.require().userId(), request));
    }

    @PostMapping("/users/{userId}/credential-email")
    @PreAuthorize("hasAuthority('system:user:create')")
    public ApiResponse<CreatedAdminUserResponse> retryCredentialEmail(@PathVariable Long userId) {
        return ApiResponse.success(authorizationManagementService.retryCredentialEmail(
                com.chareslm.shopping.security.context.CurrentUser.require().userId(), userId));
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
