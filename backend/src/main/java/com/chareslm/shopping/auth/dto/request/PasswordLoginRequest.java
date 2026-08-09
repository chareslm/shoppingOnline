package com.chareslm.shopping.auth.dto.request;

import com.chareslm.shopping.auth.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PasswordLoginRequest(
        @NotBlank @Size(max = 254) String identifier,
        @NotBlank @Size(max = 64) String password,
        @NotBlank @Size(max = 128) String deviceId,
        @NotNull DeviceType deviceType,
        @Size(max = 128) String deviceName,
        @Size(max = 64) String appVersion
) {
}
