package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 商品状态流转请求。
 * action: SUBMIT(提交审核) / PUBLISH(上架) / OFF_SHELF(下架)。
 */
public record SpuStatusRequest(
        @NotBlank
        @Pattern(regexp = "SUBMIT|PUBLISH|OFF_SHELF", message = "action must be SUBMIT, PUBLISH or OFF_SHELF")
        String action,
        @Size(max = 255) String remark
) {
}
