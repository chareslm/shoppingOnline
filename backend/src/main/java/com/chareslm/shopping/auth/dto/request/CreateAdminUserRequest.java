package com.chareslm.shopping.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateAdminUserRequest(
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,63}$", message = "invalid username") String username,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^1\\d{10}$", message = "invalid phone") String phone,
        @NotEmpty Set<Long> roleIds,
        @NotBlank @Size(max = 64) String currentPassword
) {
}
