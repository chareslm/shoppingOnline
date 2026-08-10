package com.chareslm.shopping.cart.service;

import com.chareslm.shopping.cart.dto.CartDTO;
import com.chareslm.shopping.cart.entity.Cart;

import java.math.BigDecimal;

/**
 * 购物车服务：一用户一车，按商家分组，增删改查与勾选。
 */
public interface CartService {

    /**
     * 获取或创建用户购物车。
     */
    Cart getOrCreateCart(Long userId);

    /**
     * 添加商品到购物车（同 SKU 合并数量，自动按商家分组）。
     *
     * @param price 价格快照（真实场景由成员 3 SKU 接口提供，结算时与最新价校验）
     */
    void addItem(Long userId, Long skuId, int quantity, Long shopId, BigDecimal price);

    /**
     * 更新购物项数量。
     */
    void updateQuantity(Long userId, Long itemId, int quantity);

    /**
     * 更新购物项勾选状态（1 勾选 / 0 未勾选）。
     */
    void updateChecked(Long userId, Long itemId, int checked);

    /**
     * 移除购物项（软删除 status=0）。
     */
    void removeItem(Long userId, Long itemId);

    /**
     * 查询购物车（分组展示）。
     */
    CartDTO getCart(Long userId);
}