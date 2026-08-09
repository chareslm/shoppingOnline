package com.chareslm.shopping.auth.service;

import com.chareslm.shopping.auth.dto.request.PasswordLoginRequest;
import com.chareslm.shopping.auth.dto.request.RefreshTokenRequest;
import com.chareslm.shopping.auth.dto.request.RegisterRequest;
import com.chareslm.shopping.auth.dto.response.LoginResponse;
import com.chareslm.shopping.auth.dto.response.RegisteredUserResponse;

public interface AuthService {
    RegisteredUserResponse register(RegisterRequest request);

    LoginResponse loginWithPassword(PasswordLoginRequest request, String clientIp);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(Long userId, String deviceId);
}
