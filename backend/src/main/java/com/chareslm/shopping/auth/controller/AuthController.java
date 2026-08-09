package com.chareslm.shopping.auth.controller;

import com.chareslm.shopping.auth.dto.request.PasswordLoginRequest;
import com.chareslm.shopping.auth.dto.request.LogoutRequest;
import com.chareslm.shopping.auth.dto.request.RefreshTokenRequest;
import com.chareslm.shopping.auth.dto.request.RegisterRequest;
import com.chareslm.shopping.auth.dto.response.CurrentUserResponse;
import com.chareslm.shopping.auth.dto.response.LoginResponse;
import com.chareslm.shopping.auth.dto.response.RegisteredUserResponse;
import com.chareslm.shopping.auth.service.AuthService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.security.context.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(CurrentUser.require().userId(), request.deviceId());
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser() {
        LoginUser user = CurrentUser.require();
        return ApiResponse.success(new CurrentUserResponse(user.userId(), user.username(), user.roles(), user.permissions()));
    }
}
