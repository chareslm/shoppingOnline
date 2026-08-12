package com.chareslm.shopping.cart.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新购物项勾选状态请求（1 勾选结算 / 0 未勾选）。
 */
@Getter
@Setter
public class UpdateCheckedRequest {

    @NotNull
    @Min(0)
    @Max(1)
    private Integer checked;
}