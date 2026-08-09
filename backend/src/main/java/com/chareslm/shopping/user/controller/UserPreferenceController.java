package com.chareslm.shopping.user.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.user.dto.request.UpdateUserPreferenceRequest;
import com.chareslm.shopping.user.dto.response.UserPreferenceResponse;
import com.chareslm.shopping.user.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferenceController {
    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) { this.userPreferenceService = userPreferenceService; }

    @GetMapping
    public ApiResponse<UserPreferenceResponse> getPreference() {
        return ApiResponse.success(userPreferenceService.getPreference(CurrentUser.require().userId()));
    }

    @PutMapping
    public ApiResponse<UserPreferenceResponse> updatePreference(@Valid @RequestBody UpdateUserPreferenceRequest request) {
        return ApiResponse.success(userPreferenceService.updatePreference(CurrentUser.require().userId(), request));
    }
}
