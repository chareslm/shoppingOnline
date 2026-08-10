# 交易模块数据库（成员 4）

购物车 / 订单 / 支付模块的建表脚本与说明。

## 文件说明

| 文件 | 内容 | 表 |
| --- | --- | --- |
| `001_cart.sql` | 购物车 | cart, cart_group, cart_item |
| `002_order.sql` | 订单 | order, order_item, stock_reservation, order_operation_log |
| `003_payment.sql` | 支付 | payment_order, payment_record, refund_order, reconciliation_record |
| `verify.sql` | 验证脚本 | 断言 11 张表 + 非法数据被拒 |

设计文档见 `docs/database/transaction-module.md`（字段定义、状态机、并发策略的唯一依据）。

## 执行顺序

```bash
# 1. 创建数据库（若不存在）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS shopping DEFAULT CHARACTER SET utf8mb4;"

# 2. 依次执行建表脚本
mysql -uroot -p shopping < 001_cart.sql
mysql -uroot -p shopping < 002_order.sql
mysql -uroot -p shopping < 003_payment.sql

# 3. 验证（11 张表 + 3 个非法用例被拒）
mysql -uroot -p shopping < verify.sql
```

## 依赖说明（跨模块边界）

本模块**只引用**以下表的 ID，不建表、不建外键（跨模块表可能未创建）：

| 被引用表 | 归属成员 | 引用位置 |
| --- | --- | --- |
| `user` | 成员 1（身份） | cart.user_id, order.user_id, payment_order.user_id, refund_order.user_id |
| `shop` | 成员 2（商家） | cart_group.shop_id, order.shop_id |
| `sku` | 成员 3（商品） | cart_item.sku_id, order_item.sku_id, stock_reservation.sku_id |

> 用户/商家 ID 从认证上下文获取（团队约定），不接受客户端指定。

## 关键设计

- **主键**：雪花 ID（BIGINT，应用层生成，非自增）
- **金额**：DECIMAL(10,2)；**状态**：TINYINT + CHECK 约束（MySQL 8.0.16+ 生效）
- **乐观锁**：所有业务表含 `version`，更新时 `WHERE id=? AND version=?`
- **订单状态机**：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭（超时）；1/2→6退款中→7已退款
- **库存预占**：下单创建 `stock_reservation`（status=0），支付成功 0→1，超时关闭 0→2（释放）
- **支付幂等**：`payment_no` 唯一 + `payment_record` 按 (payment_order_id, callback_type) 去重
- **模拟支付**：渠道 `MOCK_WECHAT`，预留 out_trade_no / transaction_id / prepay_id 字段，后续可升级真实微信支付
- **对账**：每日 `reconciliation_record` 对比支付单与渠道侧数据

## 验证说明

`verify.sql` 会：
1. 断言 11 张表全部存在
2. 断言非法数据被拒：`order.status=9`（CHECK 越界）、`cart_item.quantity=0`（CHECK 越界）、重复 `payment_no`（唯一键冲突）
3. 清理测试数据（不污染业务库）

> 注意：`order` 是 MySQL 保留字，SQL 中一律使用反引号 `` `order` ``。