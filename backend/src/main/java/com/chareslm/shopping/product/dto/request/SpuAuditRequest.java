package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员审核商品。
 * result: APPROVE(通过/重新通过并上架) / REJECT(驳回) / REVOKE(收回审核，商品下架)。
 */
public record SpuAuditRequest(
        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT|REVOKE", message = "result must be APPROVE, REJECT or REVOKE")
        String result,
        @Size(max = 255) String remark
) {
}
