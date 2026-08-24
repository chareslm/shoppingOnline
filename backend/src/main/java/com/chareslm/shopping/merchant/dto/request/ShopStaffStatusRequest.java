package com.chareslm.shopping.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShopStaffStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status
) {
}
