-- ============================================================
-- 交易模块 - 验证脚本
-- 文件: database/transaction/verify.sql
-- 执行: mysql -uroot -p shopping < verify.sql
-- 断言: 11 张表齐全 + 3 个非法用例被数据库拒绝
-- ============================================================

-- 1. 断言 11 张表全部存在
SELECT '1. 表数量断言(期望 11)' AS check_name;
SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'cart', 'cart_group', 'cart_item',
    'order', 'order_item', 'stock_reservation', 'order_operation_log',
    'payment_order', 'payment_record', 'refund_order', 'reconciliation_record'
  );
-- 期望: table_count = 11

-- 2. 非法用例: order.status = 9 应被 CHECK 拒绝
SELECT '2. order.status=9 应被拒' AS check_name;
INSERT INTO `order` (id, order_no, user_id, shop_id, status, total_amount, pay_amount)
VALUES (9000000000000000001, 'TEST-ORDER-1', 1, 1, 9, 100.00, 100.00);
-- 期望: 报错 ERROR 3819 (HY000): Check constraint 'chk_order_status' is violated.

-- 3. 非法用例: cart_item.quantity = 0 应被 CHECK 拒绝
SELECT '3. cart_item.quantity=0 应被拒' AS check_name;
INSERT INTO cart_item (id, cart_id, group_id, sku_id, quantity)
VALUES (9000000000000000002, 1, 1, 1, 0);
-- 期望: 报错 ERROR 3819 (HY000): Check constraint 'chk_cart_item_quantity' is violated.

-- 4. 非法用例: 重复 payment_no 应被唯一键拒绝
SELECT '4. 重复 payment_no 应被拒' AS check_name;
INSERT INTO payment_order (id, payment_no, order_id, user_id, amount, expire_time)
VALUES (9000000000000000003, 'PAY-DUP', 1, 1, 100.00, NOW());
INSERT INTO payment_order (id, payment_no, order_id, user_id, amount, expire_time)
VALUES (9000000000000000004, 'PAY-DUP', 2, 1, 100.00, NOW());
-- 期望: 第二条报错 ERROR 1062 (23000): Duplicate entry 'PAY-DUP' for key 'payment_order.uk_payment_no'.

-- 5. 清理测试数据（若前面断言失败导致有残留）
DELETE FROM payment_order WHERE payment_no = 'PAY-DUP';
DELETE FROM cart_item WHERE id = 9000000000000000002;
DELETE FROM `order` WHERE order_no = 'TEST-ORDER-1';

-- 6. 汇总
SELECT '验证完成: 若 1 显示 11 且 2/3/4 均报错, 则全部通过' AS result;