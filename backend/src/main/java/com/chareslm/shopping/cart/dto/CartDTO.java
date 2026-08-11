package com.chareslm.shopping.cart.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 购物车 DTO（分组展示）。
 */
@Getter
@Setter
public class CartDTO {

    private Long cartId;

    private List<CartGroupDTO> groups;
}