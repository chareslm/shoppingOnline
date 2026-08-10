package com.chareslm.shopping.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;

public record RegisterRequest(
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,63}$", message = "invalid username") String username,
        @Email(message = "invalid email") @Size(max = 254) String email,
        @Pattern(regexp = "^1\\d{10}$", message = "invalid phone") String phone,
        @NotBlank
        @Size(min = 12, max = 64)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
                message = "password must contain uppercase, lowercase, digit, and special characters"
        )
        String password
) {
    @AssertTrue(message = "at least one login identifier is required")
    public boolean hasLoginIdentifier() {
        return hasText(username) || hasText(email) || hasText(phone);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
