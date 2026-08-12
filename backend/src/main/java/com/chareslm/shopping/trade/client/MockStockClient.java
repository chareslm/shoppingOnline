package com.chareslm.shopping.trade.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库存模拟实现（本地开发用）。
 * <p>
 * 内存 {@code ConcurrentHashMap<Long, long[]>}（[库存, 已预占]），线程安全。
 * 未初始化 SKU 视为无库存（reserve/deduct 返回 false），测试/开发需先 {@link #initStock}。
 * 通过配置 {@code trade.stock.mock-enabled=false} 可禁用（成员 3 提供真实实现后）。
 */
@Component
@ConditionalOnProperty(name = "trade.stock.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockStockClient implements StockClient {

    private final Map<Long, long[]> stock = new ConcurrentHashMap<>();

    /**
     * 初始化 SKU 库存（默认未初始化时按 100 处理）。
     */
    public void initStock(Long skuId, int stock) {
        this.stock.put(skuId, new long[]{stock, 0});
    }

    /** 清空全部库存状态（测试隔离用）。 */
    public void reset() {
        this.stock.clear();
    }

    /** 查询 SKU 当前可售库存（库存 - 已预占），测试断言用；未初始化返回 0。 */
    public int getAvailable(Long skuId) {
        long[] s = stock.get(skuId);
        return s == null ? 0 : (int) (s[0] - s[1]);
    }

    @Override
    public synchronized boolean reserve(Long skuId, int quantity) {
        long[] s = stock.get(skuId);
        if (s == null) {
            return false;
        }
        if (s[0] - s[1] >= quantity) {
            s[1] += quantity;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean release(Long skuId, int quantity) {
        long[] s = stock.get(skuId);
        if (s == null) {
            return false;
        }
        s[1] = Math.max(0, s[1] - quantity);
        return true;
    }

    @Override
    public synchronized boolean deduct(Long skuId, int quantity) {
        long[] s = stock.get(skuId);
        if (s == null || s[0] < quantity) {
            return false;
        }
        s[0] -= quantity;
        s[1] = Math.max(0, s[1] - quantity);
        return true;
    }
}