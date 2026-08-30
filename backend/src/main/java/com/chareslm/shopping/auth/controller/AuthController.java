package com.chareslm.shopping.auth.controller;

import com.chareslm.shopping.auth.dto.request.ChangePasswordRequest;
import com.chareslm.shopping.auth.dto.request.PasswordLoginRequest;
import com.chareslm.shopping.auth.dto.request.LogoutRequest;
import com.chareslm.shopping.auth.dto.request.RefreshTokenRequest;
import com.chareslm.shopping.auth.dto.request.RegisterRequest;
import com.chareslm.shopping.auth.dto.response.CurrentUserResponse;
import com.chareslm.shopping.auth.dto.response.DeviceSessionResponse;
import com.chareslm.shopping.auth.dto.response.LoginResponse;
import com.chareslm.shopping.auth.dto.response.RegisteredUserResponse;
import com.chareslm.shopping.auth.service.AuthService;
import com.chareslm.shopping.auth.service.DeviceSessionService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.security.context.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final DeviceSessionService deviceSessionService;

    public AuthController(AuthService authService, DeviceSessionService deviceSessionService) {
        this.authService = authService;
        this.deviceSessionService = deviceSessionService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisteredUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login/password")
    public ApiResponse<LoginResponse> loginWithPassword(@Valid @RequestBody PasswordLoginRequest request,
                                                         HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.loginWithPassword(request, servletRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @RequestMapping(path = "/password", method = {RequestMethod.PUT, RequestMethod.POST})
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.require().userId(), request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(CurrentUser.require().userId(), request.deviceId());
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser() {
        LoginUser user = CurrentUser.require();
        return ApiResponse.success(new CurrentUserResponse(user.userId(), user.username(), user.roles(),
                user.permissions(), user.mustChangePassword()));
    }

    @GetMapping("/devices")
    public ApiResponse<List<DeviceSessionResponse>> devices() {
        LoginUser user = requireDeviceContext();
        return ApiResponse.success(deviceSessionService.listDevices(user.userId(), user.deviceId()));
    }

    @PostMapping("/devices/{deviceId}/revoke")
    public ApiResponse<Void> revokeDevice(@PathVariable Long deviceId) {
        LoginUser user = requireDeviceContext();
        deviceSessionService.revokeDevice(user.userId(), deviceId);
        return ApiResponse.success(null);
    }

    @PostMapping("/devices/revoke-others")
    public ApiResponse<Void> revokeOtherDevices() {
        LoginUser user = requireDeviceContext();
        deviceSessionService.revokeOtherDevices(user.userId(), user.deviceId());
        return ApiResponse.success(null);
    }

    private static LoginUser requireDeviceContext() {
        LoginUser user = CurrentUser.require();
        if (user.deviceId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
