package com.chareslm.shopping.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShopStaffAuditRequest(
        @NotBlank @Pattern(regexp = "APPROVE|REJECT") String result,
        @Size(max = 512) String remark
) {
}
