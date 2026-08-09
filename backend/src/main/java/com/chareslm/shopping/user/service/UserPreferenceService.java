package com.chareslm.shopping.user.service;

import com.chareslm.shopping.user.dto.request.UpdateUserPreferenceRequest;
import com.chareslm.shopping.user.dto.response.UserPreferenceResponse;

public interface UserPreferenceService {
    UserPreferenceResponse getPreference(Long userId);

    UserPreferenceResponse updatePreference(Long userId, UpdateUserPreferenceRequest request);
}
