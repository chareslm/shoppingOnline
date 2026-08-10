package com.chareslm.shopping.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 购物车（一用户一购物车）。
 */
@Getter
@Setter
@TableName("cart")
public class Cart extends BaseEntity {

    /** 用户 ID（引用成员1 user 表） */
    private Long userId;

    /** 1 有效 / 0 停用 */
    private Integer status;
}