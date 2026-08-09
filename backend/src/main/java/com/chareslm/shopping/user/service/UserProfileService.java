package com.chareslm.shopping.user.service;

import com.chareslm.shopping.user.dto.request.UpdateUserProfileRequest;
import com.chareslm.shopping.user.dto.response.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request);
}
