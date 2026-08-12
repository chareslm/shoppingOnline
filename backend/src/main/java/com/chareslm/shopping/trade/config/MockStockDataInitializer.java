package com.chareslm.shopping.trade.config;

import com.chareslm.shopping.trade.client.MockStockClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地开发演示库存初始化：mock 库存模式下预置演示 SKU。
 * <p>
 * {@link MockStockClient} 对未初始化 SKU 视为无库存（reserve/deduct 返回 false），
 * 无运行时入口时应用启动后下单必然失败（40011 insufficient stock）。
 * 成员 3 提供真实库存实现后，可移除本类。
 */
@Component
@ConditionalOnProperty(name = "trade.stock.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockStockDataInitializer implements ApplicationRunner {

    private final MockStockClient mockStockClient;

    public MockStockDataInitializer(MockStockClient mockStockClient) {
        this.mockStockClient = mockStockClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        mockStockClient.initStock(101L, 100);
        mockStockClient.initStock(102L, 100);
        mockStockClient.initStock(103L, 100);
        mockStockClient.initStock(104L, 100);
        mockStockClient.initStock(105L, 100);
    }
}
