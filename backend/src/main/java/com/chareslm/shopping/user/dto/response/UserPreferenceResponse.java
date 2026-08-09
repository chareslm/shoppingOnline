package com.chareslm.shopping.user.dto.response;

import java.util.Map;

public record UserPreferenceResponse(
        Long userId,
        boolean marketingEnabled,
        boolean orderNotificationEnabled,
        boolean systemNotificationEnabled,
        Map<String, Object> extraPreferences
) {
}
