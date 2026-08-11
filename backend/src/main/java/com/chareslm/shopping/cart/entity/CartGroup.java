package com.chareslm.shopping.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 购物车分组（按商家分组）。
 */
@Getter
@Setter
@TableName("cart_group")
public class CartGroup extends BaseEntity {

    /** 所属购物车 */
    private Long cartId;

    /** 商家 ID（引用成员2 shop 表） */
    private Long shopId;

    /** 1 有效 / 0 停用 */
    private Integer status;
}