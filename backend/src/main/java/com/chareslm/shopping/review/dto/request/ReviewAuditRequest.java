package com.chareslm.shopping.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewAuditRequest(
        @NotBlank
        @Pattern(regexp = "HIDE|DISPLAY", message = "action must be HIDE or DISPLAY")
        String action
) {
}
