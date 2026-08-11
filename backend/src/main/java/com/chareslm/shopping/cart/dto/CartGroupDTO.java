package com.chareslm.shopping.cart.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 购物车分组 DTO（按商家分组）。
 */
@Getter
@Setter
public class CartGroupDTO {

    private Long groupId;

    private Long shopId;

    private String shopName;

    private List<CartItemDTO> items;
}