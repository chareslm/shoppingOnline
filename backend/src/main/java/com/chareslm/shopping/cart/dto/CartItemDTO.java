package com.chareslm.shopping.cart.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 购物项 DTO。
 */
@Getter
@Setter
public class CartItemDTO {

    private Long itemId;

    private Long skuId;

    private String skuName;

    private String skuImage;

    private BigDecimal price;

    private Integer quantity;

    private Integer checked;

    private Long groupId;
}