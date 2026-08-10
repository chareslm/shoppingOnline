package com.chareslm.shopping.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 64) String currentPassword,
        @NotBlank
        @Size(min = 12, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "password must contain uppercase, lowercase, digit, and special characters"
        )
        String newPassword
) {
}
