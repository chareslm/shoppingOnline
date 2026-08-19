package com.chareslm.shopping.merchant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShopStaffRequest(
        @NotBlank(message = "请填写有效邮箱") @Email(message = "请填写有效邮箱") @Size(max = 254) String email,
        @NotBlank(message = "请填写客服显示名") @Size(max = 64, message = "客服显示名过长") String displayName,
        @Size(max = 64) @Pattern(
                regexp = "^$|^[A-Za-z][A-Za-z0-9_]{2,63}$",
                message = "用户名须以字母开头，3–64 位字母、数字或下划线")
        String username
) {
}
