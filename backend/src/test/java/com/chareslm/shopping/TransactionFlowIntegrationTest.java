package com.chareslm.shopping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.cart.service.CartService;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.payment.dto.CreatePaymentRequest;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.dto.RefundOrderDTO;
import com.chareslm.shopping.payment.dto.RefundRequest;
import com.chareslm.shopping.payment.entity.PaymentOrder;
import com.chareslm.shopping.payment.entity.PaymentRecord;
import com.chareslm.shopping.payment.mapper.PaymentOrderMapper;
import com.chareslm.shopping.payment.mapper.PaymentRecordMapper;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.chareslm.shopping.trade.dto.CreateOrderRequest;
import com.chareslm.shopping.trade.dto.OrderDTO;
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.StockReservation;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.StockReservationMapper;
import com.chareslm.shopping.trade.service.OrderService;
import com.chareslm.shopping.trade.task.OrderTimeoutTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易模块 Service 层集成测试（真实 Spring 上下文 + MySQL）。
 * <p>
 * 覆盖：购物车 → 下单（预占）→ 支付（幂等回调）→ 发货 → 确认收货；取消释放预占；超时关闭。
 * 每个测试方法事务回滚，不污染数据库；库存走 MySQL 原子更新。
 */
@SpringBootTest(properties = {
        "trade.stock.mock-enabled=false",
        "trade.payment.mock-enabled=true"
})
@Transactional
class TransactionFlowIntegrationTest {

    private static final Long USER_ID = 999001L;
    private static final Long SHOP_ID = 2001L;
    private static final Long SKU_ID = 3001L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StockReservationMapper stockReservationMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private PaymentRecordMapper paymentRecordMapper;
    @Autowired
    private OrderTimeoutTask orderTimeoutTask;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SpuMapper spuMapper;

    @BeforeEach
    void setUp() {
        // 结算校验要求 SKU 存在且所属 SPU 上架：插入真实商品记录（事务回滚，不污染数据库）
        Spu spu = new Spu();
        spu.setShopId(SHOP_ID);
        spu.setCategoryId(1L);
        spu.setName("集成测试商品");
        spu.setStatus("ON_SALE");
        spuMapper.insert(spu);
        Sku sku = new Sku();
        sku.setId(SKU_ID);
        sku.setSpuId(spu.getId());
        sku.setSkuCode("SKU-" + SKU_ID);
        sku.setPrice(PRICE);
        sku.setAvailableStock(100);
        sku.setReservedStock(0);
        sku.setSoldStock(0);
        sku.setStatus(1);
        skuMapper.insert(sku);
    }

    @Test
    void fullFlow_addToCart_checkout_pay_ship_confirm() {
        // 1. 加购物车（同 SKU 加两次验证合并）
        cartService.addItem(USER_ID, SKU_ID, 1, SHOP_ID, PRICE);
        cartService.addItem(USER_ID, SKU_ID, 1, SHOP_ID, PRICE);
        var cart = cartService.getCart(USER_ID);
        assertEquals(1, cart.getGroups().size());
        assertEquals(1, cart.getGroups().get(0).getItems().size());
        assertEquals(2, cart.getGroups().get(0).getItems().get(0).getQuantity());

        // 2. 下单：预占库存 + 订单待支付
        List<OrderDTO> orders = orderService.createOrder(USER_ID, newRequest());
        assertEquals(1, orders.size());
        OrderDTO order = orders.get(0);
        assertEquals(0, order.getStatus());
        assertEquals(new BigDecimal("200.00"), order.getPayAmount());
        List<StockReservation> reservations = reservationsOf(order.getOrderId());
        assertEquals(1, reservations.size());
        assertEquals(0, reservations.get(0).getStatus());
        assertEquals(2, reservations.get(0).getQuantity());
        assertStock(SKU_ID, 98, 2, 0);
        // 购物车已清空勾选
        assertTrue(cartService.getCart(USER_ID).getGroups().get(0).getItems().isEmpty());

        // 3. 创建支付单
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setOrderId(order.getOrderId());
        PaymentOrderDTO payment = paymentService.createPaymentOrder(USER_ID, payReq);
        assertEquals(0, payment.getStatus());
        assertEquals(new BigDecimal("200.00"), payment.getAmount());

        // 4. 模拟支付 → 回调成功：订单已支付、预占已扣减、回调记录已写
        PaymentOrderDTO paid = paymentService.mockPay(USER_ID, payment.getPaymentOrderId());
        assertEquals(1, paid.getStatus());
        assertEquals(1, orderMapper.selectById(order.getOrderId()).getStatus());
        assertEquals(1, reservationsOf(order.getOrderId()).get(0).getStatus());
        assertStock(SKU_ID, 98, 0, 2);
        long payRecords = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentOrderId, paid.getPaymentOrderId())
                .eq(PaymentRecord::getStatus, 1));
        assertEquals(1, payRecords);

        // 5. 发货 + 确认收货
        orderService.markShipped(order.getOrderId());
        assertEquals(2, orderMapper.selectById(order.getOrderId()).getStatus());
        orderService.confirmReceipt(USER_ID, order.getOrderId());
        assertEquals(3, orderMapper.selectById(order.getOrderId()).getStatus());
    }

    @Test
    void payCallback_isIdempotent() {
        OrderDTO order = createPaidOrder();
        PaymentOrder payment = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, order.getOrderId()));

        // 重复回调：应标记重复且不重复处理
        paymentService.handlePayCallback(payment.getId(), "{\"duplicate\":true}");
        List<PaymentRecord> records = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentOrderId, payment.getId()));
        assertEquals(2, records.size());
        assertTrue(records.stream().anyMatch(r -> r.getStatus() == 2));
        assertEquals(1, orderMapper.selectById(order.getOrderId()).getStatus());
    }

    @Test
    void cancelOrder_releasesReservation_and_rejectsTwice() {
        OrderDTO order = createOrderFor(USER_ID);
        orderService.cancelOrder(USER_ID, order.getOrderId());
        assertEquals(4, orderMapper.selectById(order.getOrderId()).getStatus());
        assertEquals(2, reservationsOf(order.getOrderId()).get(0).getStatus());
        assertStock(SKU_ID, 100, 0, 0);
        // 重复取消应报业务异常
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(USER_ID, order.getOrderId()));
    }

    @Test
    void timeoutTask_closesExpiredOrders_and_releasesReservation() {
        OrderDTO order = createOrderFor(USER_ID);
        // 把超时时间改到过去，模拟 30 分钟未支付
        Order entity = orderMapper.selectById(order.getOrderId());
        entity.setCloseTime(LocalDateTime.now().minusMinutes(1));
        orderMapper.updateById(entity);

        orderTimeoutTask.closeExpiredOrders();

        assertEquals(5, orderMapper.selectById(order.getOrderId()).getStatus());
        assertEquals(2, reservationsOf(order.getOrderId()).get(0).getStatus());
        assertStock(SKU_ID, 100, 0, 0);
    }

    @Test
    void mockRefundCompletion_requiresOwnerAndCompletesRefund() {
        OrderDTO order = createPaidOrder();
        RefundRequest request = new RefundRequest();
        request.setOrderId(order.getOrderId());
        request.setAmount(new BigDecimal("200.00"));
        request.setReason("集成测试退款");
        paymentService.refund(USER_ID, request);

        RefundOrderDTO refund = paymentService.listRefunds(USER_ID).get(0);
        assertThrows(BusinessException.class,
                () -> paymentService.mockCompleteRefund(USER_ID + 1, refund.getRefundId()));

        paymentService.mockCompleteRefund(USER_ID, refund.getRefundId());
        RefundOrderDTO completed = paymentService.listRefunds(USER_ID).get(0);
        assertEquals(1, completed.getStatus());
        assertEquals(7, orderMapper.selectById(order.getOrderId()).getStatus());
    }

    private OrderDTO createPaidOrder() {
        OrderDTO order = createOrderFor(USER_ID);
        CreatePaymentRequest payReq = new CreatePaymentRequest();
        payReq.setOrderId(order.getOrderId());
        PaymentOrderDTO payment = paymentService.createPaymentOrder(USER_ID, payReq);
        paymentService.mockPay(USER_ID, payment.getPaymentOrderId());
        return order;
    }

    private OrderDTO createOrderFor(Long userId) {
        cartService.addItem(userId, SKU_ID, 2, SHOP_ID, PRICE);
        return orderService.createOrder(userId, newRequest()).get(0);
    }

    private CreateOrderRequest newRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setReceiverName("张三");
        request.setReceiverPhone("13800000000");
        request.setReceiverAddress("北京市朝阳区测试路 1 号");
        request.setRemark("集成测试订单");
        return request;
    }

    private List<StockReservation> reservationsOf(Long orderId) {
        return stockReservationMapper.selectList(new LambdaQueryWrapper<StockReservation>()
                .eq(StockReservation::getOrderId, orderId));
    }

    private void assertStock(Long skuId, int available, int reserved, int sold) {
        Sku sku = skuMapper.selectById(skuId);
        assertEquals(available, sku.getAvailableStock());
        assertEquals(reserved, sku.getReservedStock());
        assertEquals(sold, sku.getSoldStock());
    }
}
