package com.chareslm.shopping.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.cart.entity.Cart;
import com.chareslm.shopping.cart.entity.CartGroup;
import com.chareslm.shopping.cart.entity.CartItem;
import com.chareslm.shopping.cart.mapper.CartGroupMapper;
import com.chareslm.shopping.cart.mapper.CartItemMapper;
import com.chareslm.shopping.cart.mapper.CartMapper;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.trade.client.StockClient;
import com.chareslm.shopping.trade.dto.CreateOrderRequest;
import com.chareslm.shopping.trade.dto.OrderDTO;
import com.chareslm.shopping.trade.dto.OrderItemDTO;
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.OrderItem;
import com.chareslm.shopping.trade.entity.OrderOperationLog;
import com.chareslm.shopping.trade.entity.StockReservation;
import com.chareslm.shopping.trade.mapper.OrderItemMapper;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.OrderOperationLogMapper;
import com.chareslm.shopping.trade.mapper.StockReservationMapper;
import com.chareslm.shopping.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务实现。
 * <p>
 * 状态机（设计文档 §4）：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭（超时）。
 * 每次状态变更写 order_operation_log；下单预占库存，取消/超时释放。
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 支付超时时间（分钟） */
    private static final int PAY_TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockReservationMapper stockReservationMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final CartMapper cartMapper;
    private final CartGroupMapper cartGroupMapper;
    private final CartItemMapper cartItemMapper;
    private final StockClient stockClient;

    @Override
    @Transactional
    public List<OrderDTO> createOrder(Long userId, CreateOrderRequest request) {
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (cart == null) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        List<CartItem> checkedItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getChecked, 1)
                .eq(CartItem::getStatus, 1));
        if (checkedItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }
        // 按商家分组拆单
        Map<Long, List<CartItem>> byShop = new LinkedHashMap<>();
        for (CartItem item : checkedItems) {
            CartGroup group = cartGroupMapper.selectById(item.getGroupId());
            if (group == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            byShop.computeIfAbsent(group.getShopId(), k -> new ArrayList<>()).add(item);
        }
        List<OrderDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : byShop.entrySet()) {
            result.add(toDTO(createOrderForShop(userId, request, entry.getKey(), entry.getValue())));
        }
        // 下单成功，清空勾选购物项（软删除）
        for (CartItem item : checkedItems) {
            item.setStatus(0);
            cartItemMapper.updateById(item);
        }
        return result;
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(4);
        order.setCancelReason("用户取消");
        orderMapper.updateById(order);
        releaseReservations(orderId);
        writeLog(orderId, 1, userId, "CANCEL", 0, 4, "用户取消订单");
    }

    @Override
    @Transactional
    public void confirmReceipt(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(3);
        order.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(order);
        writeLog(orderId, 1, userId, "COMPLETE", 2, 3, "用户确认收货");
    }

    @Override
    @Transactional
    public void markShipped(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(2);
        orderMapper.updateById(order);
        writeLog(orderId, 3, null, "SHIP", 1, 2, "商家发货");
    }

    @Override
    public OrderDTO getOrder(Long userId, Long orderId) {
        return toDTO(requireOwnedOrder(userId, orderId));
    }

    @Override
    public List<OrderDTO> getMyOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
        return orders.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public void markPaid(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 预占 0→1（已扣减）
        List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>()
                        .eq(StockReservation::getOrderId, orderId)
                        .eq(StockReservation::getStatus, 0));
        for (StockReservation reservation : reservations) {
            stockClient.deduct(reservation.getSkuId(), reservation.getQuantity());
            reservation.setStatus(1);
            stockReservationMapper.updateById(reservation);
        }
        writeLog(orderId, 2, null, "PAY", 0, 1, "支付成功");
    }

    /**
     * 为单个商家创建订单：订单 + 订单项 + 库存预占 + 操作日志。
     * 预占失败时补偿释放已预占的 SKU 后抛异常（事务回滚）。
     */
    private Order createOrderForShop(Long userId, CreateOrderRequest request, Long shopId, List<CartItem> items) {
        BigDecimal totalAmount = items.stream()
                .map(i -> i.getPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal freightAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount.add(freightAmount).subtract(discountAmount);
        LocalDateTime closeTime = LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setStatus(0);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(freightAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setRemark(request.getRemark());
        order.setCloseTime(closeTime);
        orderMapper.insert(order);

        Map<Long, Integer> reserved = new HashMap<>();
        try {
            for (CartItem item : items) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setSkuId(item.getSkuId());
                orderItem.setPrice(item.getPriceSnapshot());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setTotalAmount(item.getPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())));
                orderItem.setStatus(0);
                orderItemMapper.insert(orderItem);

                if (!stockClient.reserve(item.getSkuId(), item.getQuantity())) {
                    throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
                }
                reserved.put(item.getSkuId(), item.getQuantity());

                StockReservation reservation = new StockReservation();
                reservation.setOrderId(order.getId());
                reservation.setSkuId(item.getSkuId());
                reservation.setQuantity(item.getQuantity());
                reservation.setStatus(0);
                reservation.setExpireTime(closeTime);
                stockReservationMapper.insert(reservation);
            }
        } catch (BusinessException e) {
            // 补偿释放已预占的 SKU（内存模拟实现无法随事务回滚）
            reserved.forEach(stockClient::release);
            throw e;
        }
        writeLog(order.getId(), 1, userId, "CREATE", null, 0, "下单创建");
        return order;
    }

    /**
     * 释放订单全部预占（status 0→2）。
     */
    private void releaseReservations(Long orderId) {
        List<StockReservation> reservations = stockReservationMapper.selectList(
                new LambdaQueryWrapper<StockReservation>()
                        .eq(StockReservation::getOrderId, orderId)
                        .eq(StockReservation::getStatus, 0));
        for (StockReservation reservation : reservations) {
            stockClient.release(reservation.getSkuId(), reservation.getQuantity());
            reservation.setStatus(2);
            stockReservationMapper.updateById(reservation);
        }
    }

    private Order requireOwnedOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void writeLog(Long orderId, int operatorType, Long operatorId, String action,
                          Integer fromStatus, Integer toStatus, String remark) {
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderId(orderId);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderOperationLogMapper.insert(log);
    }

    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setFreightAmount(order.getFreightAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setPayAmount(order.getPayAmount());
        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setReceiverAddress(order.getReceiverAddress());
        dto.setRemark(order.getRemark());
        dto.setPayTime(order.getPayTime());
        dto.setCloseTime(order.getCloseTime());
        dto.setFinishTime(order.getFinishTime());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        dto.setItems(items.stream().map(this::toItemDTO).toList());
        return dto;
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setItemId(item.getId());
        dto.setSkuId(item.getSkuId());
        dto.setSkuName(item.getSkuName());
        dto.setSkuImage(item.getSkuImage());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setTotalAmount(item.getTotalAmount());
        dto.setStatus(item.getStatus());
        return dto;
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}