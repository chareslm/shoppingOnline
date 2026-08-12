package com.chareslm.shopping.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 添加商品到购物车请求。
 */
@Getter
@Setter
public class AddCartItemRequest {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private Long shopId;

    /** 价格快照（真实场景由成员 3 SKU 接口提供，结算时与最新价校验） */
    @NotNull
    private BigDecimal price;
}