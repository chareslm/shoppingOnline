package com.chareslm.shopping.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单项。
 */
@Getter
@Setter
@TableName("order_item")
public class OrderItem extends BaseEntity {

    /** 所属订单 */
    private Long orderId;

    /** SKU ID（引用成员3 sku 表） */
    private Long skuId;

    /** 商品名快照 */
    private String skuName;

    /** 商品图快照 */
    private String skuImage;

    /** 成交单价快照 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计 = price × quantity */
    private BigDecimal totalAmount;

    /** 0 正常 / 1 退款中 / 2 已退款 */
    private Integer status;
}