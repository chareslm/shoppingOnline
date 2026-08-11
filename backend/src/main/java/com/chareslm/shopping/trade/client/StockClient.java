package com.chareslm.shopping.trade.client;

/**
 * 跨模块库存接口（成员 3 负责真实实现）。
 * <p>
 * 设计文档 §5.3：下单时预占（reserved+?），支付成功扣减（stock-?），超时关闭释放（reserved-?）。
 * 当前使用 {@link MockStockClient} 内存模拟，成员 3 实现后替换为真实调用。
 */
public interface StockClient {

    /**
     * 预占库存：库存充足则 reserved + quantity，返回 true；不足返回 false。
     */
    boolean reserve(Long skuId, int quantity);

    /**
     * 释放预占：reserved - quantity（超时关单回补）。
     */
    boolean release(Long skuId, int quantity);

    /**
     * 扣减库存：stock - quantity 且 reserved - quantity（支付成功后正式扣减）。
     */
    boolean deduct(Long skuId, int quantity);
}