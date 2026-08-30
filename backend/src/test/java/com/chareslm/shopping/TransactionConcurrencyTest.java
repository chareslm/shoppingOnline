package com.chareslm.shopping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.cart.entity.Cart;
import com.chareslm.shopping.cart.entity.CartGroup;
import com.chareslm.shopping.cart.entity.CartItem;
import com.chareslm.shopping.cart.mapper.CartGroupMapper;
import com.chareslm.shopping.cart.mapper.CartItemMapper;
import com.chareslm.shopping.cart.mapper.CartMapper;
import com.chareslm.shopping.cart.service.CartService;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.payment.dto.CreatePaymentRequest;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.dto.RefundRequest;
import com.chareslm.shopping.payment.entity.PaymentOrder;
import com.chareslm.shopping.payment.entity.PaymentRecord;
import com.chareslm.shopping.payment.entity.RefundOrder;
import com.chareslm.shopping.payment.mapper.PaymentOrderMapper;
import com.chareslm.shopping.payment.mapper.PaymentRecordMapper;
import com.chareslm.shopping.payment.mapper.RefundOrderMapper;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.chareslm.shopping.trade.dto.CreateOrderRequest;
import com.chareslm.shopping.trade.dto.OrderDTO;
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.OrderItem;
import com.chareslm.shopping.trade.entity.OrderOperationLog;
import com.chareslm.shopping.trade.entity.StockReservation;
import com.chareslm.shopping.trade.mapper.OrderItemMapper;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.OrderOperationLogMapper;
import com.chareslm.shopping.trade.mapper.StockReservationMapper;
import com.chareslm.shopping.trade.service.OrderService;
import com.chareslm.shopping.trade.task.OrderTimeoutTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易模块并发集成测试（真实 Spring 上下文 + MySQL，事务真实提交）。
 * <p>
 * 覆盖审查要求：并发支付回调幂等、支付与超时竞争、并发退款不超退。
 * 注意：本测试类不使用 @Transactional（并发子线程读不到主线程未提交数据），
 * 每个测试使用独立用户并在 tearDown 中清理数据。
 */
@SpringBootTest(properties = "trade.stock.mock-enabled=false")
class TransactionConcurrencyTest {

    private static final Long USER_ID = 999011L;
    private static final Long SHOP_ID = 2011L;
    private static final Long SKU_ID = 3011L;
    private static final Long SECOND_SHOP_ID = 2012L;
    private static final Long SECOND_SKU_ID = 3012L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private Long spuId;
    private Long secondSpuId;

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderOperationLogMapper orderOperationLogMapper;
    @Autowired
    private StockReservationMapper stockReservationMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private CartGroupMapper cartGroupMapper;
    @Autowired
    private CartItemMapper cartItemMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private PaymentRecordMapper paymentRecordMapper;
    @Autowired
    private RefundOrderMapper refundOrderMapper;
    @Autowired
    private OrderTimeoutTask orderTimeoutTask;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SpuMapper spuMapper;

    @BeforeEach
    void setUp() {
        // 结算校验要求 SKU 存在且所属 SPU 上架：插入真实商品记录（tearDown 清理）
        Spu spu = new Spu();
        spu.setShopId(SHOP_ID);
        spu.setCategoryId(1L);
        spu.setName("并发测试商品");
        spu.setStatus("ON_SALE");
        spuMapper.insert(spu);
        spuId = spu.getId();
        Sku sku = new Sku();
        sku.setId(SKU_ID);
        sku.setSpuId(spuId);
        sku.setSkuCode("SKU-" + SKU_ID);
        sku.setPrice(PRICE);
        sku.setAvailableStock(100);
        sku.setReservedStock(0);
        sku.setSoldStock(0);
        sku.setStatus(1);
        skuMapper.insert(sku);
    }

    @AfterEach
    void tearDown() {
        cleanup(USER_ID);
        skuMapper.deleteById(SKU_ID);
        if (spuId != null) {
            spuMapper.deleteById(spuId);
        }
        skuMapper.deleteById(SECOND_SKU_ID);
        if (secondSpuId != null) {
            spuMapper.deleteById(secondSpuId);
        }
        executor.shutdownNow();
    }

    @Test
    void concurrentPayCallback_onlyOneProcessed() throws Exception {
        OrderDTO order = createOrderFor(USER_ID);
        PaymentOrder payment = createPaymentFor(USER_ID, order.getOrderId());

        int threads = 4;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    paymentService.handlePayCallback(payment.getId(), "{\"concurrent\":true}");
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        assertEquals(0, errors.get(), "并发回调不应有异常");

        // 唯一约束保证只有一条 status=1 处理记录
        long processed = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentOrderId, payment.getId())
                .eq(PaymentRecord::getCallbackType, "PAY")
                .eq(PaymentRecord::getStatus, 1));
        assertEquals(1, processed, "并发回调只能有一条处理记录");
        // 订单只被支付一次（status=1）
        assertEquals(1, orderMapper.selectById(order.getOrderId()).getStatus());
        // 库存只扣减一次（100 - 2 = 98 可用）
        Sku stock = skuMapper.selectById(SKU_ID);
        assertEquals(98, stock.getAvailableStock(), "可售库存应只扣减一次");
        assertEquals(0, stock.getReservedStock(), "支付后预占库存应转为已售");
        assertEquals(2, stock.getSoldStock(), "已售库存应只增加一次");
    }

    @Test
    void concurrentPayAndTimeout_onlyOneWins() throws Exception {
        OrderDTO order = createOrderFor(USER_ID);
        PaymentOrder payment = createPaymentFor(USER_ID, order.getOrderId());
        // 把超时时间改到过去，让超时任务可以命中
        Order entity = orderMapper.selectById(order.getOrderId());
        entity.setCloseTime(LocalDateTime.now().minusMinutes(1));
        orderMapper.updateById(entity);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        executor.submit(() -> {
            try {
                start.await();
                paymentService.handlePayCallback(payment.getId(), "{\"race\":true}");
            } catch (Exception ignored) {
                // 竞争失败方（已超时关闭）抛异常是允许的
            } finally {
                done.countDown();
            }
        });
        executor.submit(() -> {
            try {
                start.await();
                orderTimeoutTask.closeExpiredOrders();
            } catch (Exception ignored) {
                // 忽略
            } finally {
                done.countDown();
            }
        });
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));

        // 最终订单状态必须是 1（已支付）或 5（超时关闭），且预占状态一致：
        // 已支付 → 预占已扣减(status=1)；超时关闭 → 预占已释放(status=2)
        int status = orderMapper.selectById(order.getOrderId()).getStatus();
        assertTrue(status == 1 || status == 5, "支付与超时竞争后订单状态必须是 1 或 5，实际: " + status);
        List<StockReservation> reservations = reservationsOf(order.getOrderId());
        for (StockReservation r : reservations) {
            if (status == 1) {
                assertEquals(1, r.getStatus(), "已支付则预占必须已扣减");
            } else {
                assertEquals(2, r.getStatus(), "超时关闭则预占必须已释放");
            }
        }
    }

    @Test
    void concurrentRefund_noOverRefund() throws Exception {
        OrderDTO order = createPaidOrderFor(USER_ID);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    RefundRequest request = new RefundRequest();
                    request.setOrderId(order.getOrderId());
                    request.setAmount(new BigDecimal("200.00"));
                    paymentService.refund(USER_ID, request);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    // InterruptedException 等：视为失败
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));

        // 悲观锁 + 累计校验：并发两个全额退款只能成功一个
        assertEquals(1, success.get(), "并发全额退款只能成功一个");
        assertEquals(1, rejected.get(), "另一个必须被拒绝");
        long refunds = refundOrderMapper.selectCount(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, order.getOrderId()));
        assertEquals(1, refunds, "只能存在一条退款单");
    }

    @Test
    void multiShopFailure_rollsBackEarlierShopsMysqlReservation() {
        Spu secondSpu = new Spu();
        secondSpu.setShopId(SECOND_SHOP_ID);
        secondSpu.setCategoryId(1L);
        secondSpu.setName("跨店失败商品");
        secondSpu.setStatus("ON_SALE");
        spuMapper.insert(secondSpu);
        secondSpuId = secondSpu.getId();

        Sku secondSku = new Sku();
        secondSku.setId(SECOND_SKU_ID);
        secondSku.setSpuId(secondSpuId);
        secondSku.setSkuCode("SKU-" + SECOND_SKU_ID);
        secondSku.setPrice(PRICE);
        secondSku.setAvailableStock(0);
        secondSku.setReservedStock(0);
        secondSku.setSoldStock(0);
        secondSku.setStatus(1);
        skuMapper.insert(secondSku);

        cartService.addItem(USER_ID, SKU_ID, 2, SHOP_ID, PRICE);
        cartService.addItem(USER_ID, SECOND_SKU_ID, 1, SECOND_SHOP_ID, PRICE);

        org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class,
                () -> orderService.createOrder(USER_ID, newRequest()));

        Sku firstStock = skuMapper.selectById(SKU_ID);
        assertEquals(100, firstStock.getAvailableStock(), "前一店铺可售库存必须随整笔事务回滚");
        assertEquals(0, firstStock.getReservedStock(), "前一店铺不得遗留预占库存");
        assertEquals(0L, orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, USER_ID)), "跨店失败不得留下半完成订单");
    }

    private OrderDTO createOrderFor(Long userId) {
        cartService.addItem(userId, SKU_ID, 2, SHOP_ID, PRICE);
        return orderService.createOrder(userId, newRequest()).get(0);
    }

    private OrderDTO createPaidOrderFor(Long userId) {
        OrderDTO order = createOrderFor(userId);
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setOrderId(order.getOrderId());
        PaymentOrderDTO payment = paymentService.createPaymentOrder(userId, payReq);
        paymentService.mockPay(userId, payment.getPaymentOrderId());
        return order;
    }

    private PaymentOrder createPaymentFor(Long userId, Long orderId) {
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setOrderId(orderId);
        PaymentOrderDTO dto = paymentService.createPaymentOrder(userId, payReq);
        return paymentOrderMapper.selectById(dto.getPaymentOrderId());
    }

    private CreateOrderRequest newRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setReceiverName("并发测试");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("北京市朝阳区并发路 1 号");
        return request;
    }

    private List<StockReservation> reservationsOf(Long orderId) {
        return stockReservationMapper.selectList(new LambdaQueryWrapper<StockReservation>()
                .eq(StockReservation::getOrderId, orderId));
    }

    private void cleanup(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId));
        for (Order order : orders) {
            orderOperationLogMapper.delete(new LambdaQueryWrapper<OrderOperationLog>()
                    .eq(OrderOperationLog::getOrderId, order.getId()));
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            stockReservationMapper.delete(new LambdaQueryWrapper<StockReservation>()
                    .eq(StockReservation::getOrderId, order.getId()));
            refundOrderMapper.delete(new LambdaQueryWrapper<RefundOrder>()
                    .eq(RefundOrder::getOrderId, order.getId()));
            PaymentOrder payment = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getOrderId, order.getId()));
            if (payment != null) {
                paymentRecordMapper.delete(new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPaymentOrderId, payment.getId()));
                paymentOrderMapper.deleteById(payment.getId());
            }
        }
        orderMapper.delete(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));
        for (Cart cart : carts) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getCartId, cart.getId()));
            cartGroupMapper.delete(new LambdaQueryWrapper<CartGroup>()
                    .eq(CartGroup::getCartId, cart.getId()));
            cartMapper.deleteById(cart.getId());
        }
    }
}
