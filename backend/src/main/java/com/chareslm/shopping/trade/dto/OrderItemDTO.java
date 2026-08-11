package com.chareslm.shopping.trade.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单项 DTO。
 */
@Getter
@Setter
public class OrderItemDTO {

    private Long itemId;

    private Long skuId;

    private String skuName;

    private String skuImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;

    private Integer status;
}