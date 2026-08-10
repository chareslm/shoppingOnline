package com.chareslm.shopping.trade.service;

import com.chareslm.shopping.trade.dto.CreateOrderRequest;
import com.chareslm.shopping.trade.dto.OrderDTO;

import java.util.List;

/**
 * 订单服务：按商家拆单、状态机流转、库存预占与释放。
 * <p>
 * 状态机：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭（超时）；1/2→6退款中→7已退款。
 */
public interface OrderService {

    /**
     * 下单：结算当前用户全部勾选购物项，按商家拆单，创建订单+订单项+库存预占。
     *
     * @return 生成的订单列表（按商家拆单可能多个）
     */
    List<OrderDTO> createOrder(Long userId, CreateOrderRequest request);

    /**
     * 取消订单（仅待支付 0→4），释放库存预占。
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 确认收货（2→3）。
     */
    void confirmReceipt(Long userId, Long orderId);

    /**
     * 商家发货（1→2，商家/管理员操作）。
     */
    void markShipped(Long orderId);

    /**
     * 查询订单详情。
     */
    OrderDTO getOrder(Long userId, Long orderId);

    /**
     * 查询我的订单列表。
     */
    List<OrderDTO> getMyOrders(Long userId);

    /**
     * 支付成功回调：订单 0→1，预占 0→1（系统操作，由 PaymentService 调用）。
     */
    void markPaid(Long orderId);
}