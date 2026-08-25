package com.chareslm.shopping.statistics;

import com.chareslm.shopping.statistics.mapper.StatisticsMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class StatisticsMapperIntegrationTest {
    private static final LocalDateTime START = LocalDateTime.of(2098, 8, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2098, 8, 3, 0, 0);
    private static final long USER_ONE = 910_001L;
    private static final long USER_TWO = 910_002L;
    private static final long USER_BOUNDARY = 910_003L;
    private static final long ADMIN_ONLY = 910_004L;
    private static final long SHOP_ONE = 920_001L;
    private static final long SHOP_TWO = 920_002L;

    @Autowired
    private StatisticsMapper statisticsMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Test
    void exactQueriesMatchAuthorityTablesAndHalfOpenRange() {
        var before = statisticsMapper.selectPlatformOverview(START, END);
        seedUsers();
        seedOrdersPaymentsAndRefunds();
        seedProductsReviewsAndSearches();
        sqlSessionTemplate.clearCache();

        var platform = statisticsMapper.selectPlatformOverview(START, END);
        assertEquals(before.getNewUserCount() + 2, platform.getNewUserCount());
        assertEquals(before.getActiveUserCount() + 3, platform.getActiveUserCount());
        assertEquals(before.getPaidOrderCount() + 2, platform.getPaidOrderCount());
        assertEquals(before.getPaidBuyerCount() + 2, platform.getPaidBuyerCount());
        assertMoney(before.getGrossPaidAmount().add(new BigDecimal("150.00")), platform.getGrossPaidAmount());
        assertMoney(before.getSuccessfulRefundAmount().add(new BigDecimal("130.00")),
                platform.getSuccessfulRefundAmount());
        assertEquals(before.getOnSaleProductCount() + 3, platform.getOnSaleProductCount());
        assertEquals(before.getSearchCount() + 2, platform.getSearchCount());
        assertEquals(before.getDisplayedReviewCount() + 1, platform.getDisplayedReviewCount());

        BigDecimal manualGross = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount), 0.00)
                FROM payment_order
                WHERE status IN (1, 4) AND pay_time >= ? AND pay_time < ?
                """, BigDecimal.class, START, END);
        assertMoney(manualGross, platform.getGrossPaidAmount());

        var shop = statisticsMapper.selectShopOverview(SHOP_ONE, START, END);
        assertEquals(1, shop.getPaidOrderCount());
        assertEquals(1, shop.getPaidBuyerCount());
        assertMoney(new BigDecimal("100.00"), shop.getGrossPaidAmount());
        assertMoney(new BigDecimal("130.00"), shop.getSuccessfulRefundAmount());
        assertEquals(2, shop.getSoldQuantity());
        assertEquals(2, shop.getOnSaleProductCount());
        assertEquals(2, shop.getDisplayedReviewCount());
        assertMoney(new BigDecimal("4.50"), shop.getAverageRating());

        var platformTrends = statisticsMapper.selectPlatformTrends(START, END);
        assertEquals(2, platformTrends.size());
        assertEquals(LocalDate.of(2098, 8, 1), platformTrends.get(0).getMetricDate());
        assertEquals(LocalDate.of(2098, 8, 2), platformTrends.get(1).getMetricDate());
        assertMoney(new BigDecimal("100.00"), platformTrends.get(0).getGrossPaidAmount());
        assertMoney(new BigDecimal("130.00"), platformTrends.get(1).getSuccessfulRefundAmount());

        var shopTrends = statisticsMapper.selectShopTrends(SHOP_ONE, START, END);
        assertEquals(2, shopTrends.size());
        assertEquals(2, shopTrends.get(0).getSoldQuantity());
        assertMoney(new BigDecimal("130.00"), shopTrends.get(1).getSuccessfulRefundAmount());

        var user = statisticsMapper.selectUserOverview(USER_ONE, START, END);
        assertEquals(1, user.getPaidOrderCount());
        assertMoney(new BigDecimal("100.00"), user.getGrossPaidAmount());
        assertMoney(new BigDecimal("130.00"), user.getSuccessfulRefundAmount());
        assertEquals(1, user.getDisplayedReviewCount());

        assertEquals(1, permissionGrantCount("statistics:platform:view", "SUPER_ADMIN"));
        assertEquals(1, permissionGrantCount("statistics:shop:view", "MERCHANT_OWNER"));
        assertEquals(0, permissionGrantCount("statistics:shop:view", "CUSTOMER_SERVICE"));
        assertEquals(1, permissionGrantCount("statistics:self:view", "USER"));
    }

    private void seedUsers() {
        insertUser(USER_ONE, "stats_user_one", "ACTIVE", LocalDateTime.of(2098, 8, 1, 0, 0));
        insertUser(USER_TWO, "stats_user_two", "ACTIVE", LocalDateTime.of(2098, 8, 2, 12, 0));
        insertUser(USER_BOUNDARY, "stats_user_boundary", "ACTIVE", END);
        insertUser(ADMIN_ONLY, "stats_admin_only", "ACTIVE", LocalDateTime.of(2098, 8, 1, 9, 0));
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM `role` WHERE code = 'USER'
                """, USER_ONE);
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM `role` WHERE code = 'USER'
                """, USER_TWO);
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM `role` WHERE code = 'USER'
                """, USER_BOUNDARY);
    }

    private void insertUser(long id, String username, String status, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, status, created_at, updated_at)
                VALUES (?, ?, 'test-only-hash', ?, ?, ?)
                """, id, username, status, createdAt, createdAt);
    }

    private void seedOrdersPaymentsAndRefunds() {
        insertOrder(930_000L, "STATS-OLD", USER_ONE, SHOP_ONE, new BigDecimal("200.00"));
        insertOrder(930_001L, "STATS-ONE", USER_ONE, SHOP_ONE, new BigDecimal("100.00"));
        insertOrder(930_002L, "STATS-TWO", USER_TWO, SHOP_TWO, new BigDecimal("50.00"));
        insertOrder(930_003L, "STATS-END", USER_ONE, SHOP_ONE, new BigDecimal("999.00"));

        insertPayment(940_000L, "PAY-STATS-OLD", 930_000L, USER_ONE, "200.00", 1,
                LocalDateTime.of(2098, 7, 31, 20, 0));
        insertPayment(940_001L, "PAY-STATS-ONE", 930_001L, USER_ONE, "100.00", 4,
                LocalDateTime.of(2098, 8, 1, 10, 0));
        insertPayment(940_002L, "PAY-STATS-TWO", 930_002L, USER_TWO, "50.00", 1,
                LocalDateTime.of(2098, 8, 2, 10, 0));
        insertPayment(940_003L, "PAY-STATS-END", 930_003L, USER_ONE, "999.00", 1, END);

        jdbcTemplate.update("""
                INSERT INTO order_item (id, order_id, sku_id, sku_name, price, quantity, total_amount, status)
                VALUES (950001, 930001, 960001, '统计商品', 50.00, 2, 100.00, 0),
                       (950002, 930002, 960002, '其他店商品', 50.00, 1, 50.00, 0),
                       (950003, 930003, 960003, '边界商品', 999.00, 1, 999.00, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO refund_order
                    (id, refund_no, payment_order_id, order_id, user_id, amount, status, refund_time)
                VALUES (970001, 'REF-STATS-ONE', 940000, 930000, ?, 130.00, 1, ?),
                       (970002, 'REF-STATS-PENDING', 940001, 930001, ?, 20.00, 0, NULL)
                """, USER_ONE, LocalDateTime.of(2098, 8, 2, 8, 0), USER_ONE);
    }

    private void insertOrder(long id, String orderNo, long userId, long shopId, BigDecimal amount) {
        jdbcTemplate.update("""
                INSERT INTO `order`
                    (id, order_no, user_id, shop_id, status, total_amount, freight_amount,
                     discount_amount, pay_amount, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, ?, 0.00, 0.00, ?, ?, ?)
                """, id, orderNo, userId, shopId, amount, amount, START.minusDays(1), START.minusDays(1));
    }

    private void insertPayment(long id, String paymentNo, long orderId, long userId,
                               String amount, int status, LocalDateTime payTime) {
        jdbcTemplate.update("""
                INSERT INTO payment_order
                    (id, payment_no, order_id, user_id, amount, status, pay_channel,
                     pay_time, expire_time, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'MOCK_WECHAT', ?, ?, ?, ?)
                """, id, paymentNo, orderId, userId, new BigDecimal(amount), status, payTime,
                payTime.plusMinutes(30), payTime.minusMinutes(1), payTime);
    }

    private void seedProductsReviewsAndSearches() {
        jdbcTemplate.update("""
                INSERT INTO spu (id, shop_id, category_id, name, status) VALUES
                    (980001, ?, 1, '统计在售一', 'ON_SALE'),
                    (980002, ?, 1, '统计在售二', 'ON_SALE'),
                    (980003, ?, 1, '其他店在售', 'ON_SALE'),
                    (980004, ?, 1, '统计下架', 'OFF_SALE')
                """, SHOP_ONE, SHOP_ONE, SHOP_TWO, SHOP_ONE);
        jdbcTemplate.update("""
                INSERT INTO review
                    (id, order_id, order_item_id, spu_id, sku_id, user_id, shop_id,
                     rating, content, status, created_at, updated_at)
                VALUES
                    (990001, 930001, 950001, 980001, 960001, ?, ?, 5, '区间评价', 'DISPLAYED', ?, ?),
                    (990002, 930000, 950010, 980002, 960010, ?, ?, 4, '历史评价', 'DISPLAYED', ?, ?),
                    (990003, 930002, 950002, 980003, 960002, ?, ?, 1, '隐藏评价', 'HIDDEN', ?, ?)
                """, USER_ONE, SHOP_ONE, START.plusHours(2), START.plusHours(2),
                USER_ONE, SHOP_ONE, START.minusDays(2), START.minusDays(2),
                USER_TWO, SHOP_TWO, START.plusHours(3), START.plusHours(3));
        jdbcTemplate.update("""
                INSERT INTO search_log (id, keyword, user_id, created_at) VALUES
                    (995001, '统计', ?, ?),
                    (995002, '报表', NULL, ?),
                    (995003, '边界', ?, ?)
                """, USER_ONE, START.plusHours(1), START.plusDays(1).plusHours(1), USER_ONE, END);
    }

    private int permissionGrantCount(String permissionCode, String roleCode) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_permission rp
                JOIN `role` r ON r.id = rp.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE r.code = ? AND p.code = ?
                """, Integer.class, roleCode, permissionCode);
        return count == null ? 0 : count;
    }

    private void assertMoney(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
