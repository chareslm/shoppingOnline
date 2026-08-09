package com.chareslm.shopping.user.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.user.dto.request.UpdateUserProfileRequest;
import com.chareslm.shopping.user.dto.response.UserProfileResponse;
import com.chareslm.shopping.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/profile")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) { this.userProfileService = userProfileService; }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userProfileService.getProfile(CurrentUser.require().userId()));
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userProfileService.updateProfile(CurrentUser.require().userId(), request));
    }
}
