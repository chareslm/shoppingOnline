package com.chareslm.shopping.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AssignUserRolesRequest(@NotEmpty Set<Long> roleIds, @NotBlank String currentPassword) {
}
