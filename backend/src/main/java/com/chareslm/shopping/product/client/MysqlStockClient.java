package com.chareslm.shopping.product.client;

import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.trade.client.StockClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 真实库存实现（成员3 商品模块）。
 * <p>
 * 基于 MySQL `sku` 表的三段库存字段：
 * <ul>
 *   <li>reserve：可售充足则 available_stock 减、reserved_stock 增（下单预占）</li>
 *   <li>release：reserved_stock 减、available_stock 增（超时关单回补）</li>
 *   <li>deduct：available_stock 减、reserved_stock 减、sold_stock 增（支付成功扣减）</li>
 * </ul>
 * 所有操作使用单条条件 UPDATE 原子完成，禁止先查再改。
 * <p>
 * 通过 {@code trade.stock.mock-enabled=false} 启用，替换 {@code MockStockClient} 内存模拟。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trade.stock.mock-enabled", havingValue = "false")
public class MysqlStockClient implements StockClient {

    private final SkuMapper skuMapper;

    @Override
    public boolean reserve(Long skuId, int quantity) {
        return skuMapper.reserveStock(skuId, quantity) > 0;
    }

    @Override
    public boolean release(Long skuId, int quantity) {
        return skuMapper.releaseStock(skuId, quantity) > 0;
    }

    @Override
    public boolean deduct(Long skuId, int quantity) {
        return skuMapper.deductStock(skuId, quantity) > 0;
    }
}
