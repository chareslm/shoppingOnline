package com.chareslm.shopping.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 购物项。
 */
@Getter
@Setter
@TableName("cart_item")
public class CartItem extends BaseEntity {

    /** 所属购物车 */
    private Long cartId;

    /** 所属分组 */
    private Long groupId;

    /** SKU ID（引用成员3 sku 表） */
    private Long skuId;

    /** 数量 */
    private Integer quantity;

    /** 1 勾选结算 / 0 未勾选 */
    private Integer checked;

    /** 加入购物车时价格快照，结算时与最新价校验 */
    private BigDecimal priceSnapshot;

    /** 1 有效 / 0 已移除 */
    private Integer status;
}