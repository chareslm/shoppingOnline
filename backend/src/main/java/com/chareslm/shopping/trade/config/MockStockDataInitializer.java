package com.chareslm.shopping.trade.config;

import com.chareslm.shopping.trade.client.MockStockClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 本地开发演示库存初始化：mock 库存模式下应用启动时从 {@code sku} 表加载全部启用 SKU 的可售库存。
 * <p>
 * {@link MockStockClient} 对未初始化 SKU 视为无库存（reserve/deduct 返回 false），
 * 无运行时入口时应用启动后下单必然失败（40011 insufficient stock）。
 * 这里通过 JDBC 直读 sku 表（仅本地演示用途，不引入跨模块 client 接口；
 * 通过 JdbcTemplate 访问共享数据库，不 import product 模块类）。
 * 成员 3 提供真实库存实现后，可移除本类。
 */
@Component
@ConditionalOnProperty(name = "trade.stock.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockStockDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MockStockDataInitializer.class);

    private final MockStockClient mockStockClient;
    private final JdbcTemplate jdbcTemplate;

    public MockStockDataInitializer(MockStockClient mockStockClient, JdbcTemplate jdbcTemplate) {
        this.mockStockClient = mockStockClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, available_stock FROM sku WHERE status = 1");
        for (Map<String, Object> row : rows) {
            long skuId = ((Number) row.get("id")).longValue();
            Number stock = (Number) row.get("available_stock");
            mockStockClient.initStock(skuId, stock == null ? 0 : stock.intValue());
        }
        log.info("Mock stock initialized for {} SKU(s) from sku table", rows.size());
    }
}
