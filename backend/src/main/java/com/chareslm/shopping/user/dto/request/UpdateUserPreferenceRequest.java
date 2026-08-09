package com.chareslm.shopping.user.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateUserPreferenceRequest(
        @NotNull Boolean marketingEnabled,
        @NotNull Boolean orderNotificationEnabled,
        @NotNull Boolean systemNotificationEnabled,
        Map<String, Object> extraPreferences
) {
}
