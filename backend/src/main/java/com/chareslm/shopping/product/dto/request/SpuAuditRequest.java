package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员审核商品。
 * result: APPROVE(通过) / REJECT(驳回)。
 */
public record SpuAuditRequest(
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT", message = "result must be APPROVE or REJECT")
        String result,
        @Size(max = 255) String remark
) {
}
