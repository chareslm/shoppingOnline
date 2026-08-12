package com.chareslm.shopping.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新购物项数量请求。
 */
@Getter
@Setter
public class UpdateQuantityRequest {

    @NotNull
    @Min(1)
    private Integer quantity;
}