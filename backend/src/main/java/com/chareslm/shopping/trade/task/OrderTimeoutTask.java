package com.chareslm.shopping.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.trade.client.StockClient;
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.OrderOperationLog;
import com.chareslm.shopping.trade.entity.StockReservation;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.OrderOperationLogMapper;
import com.chareslm.shopping.trade.mapper.StockReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时关闭定时任务（设计文档 §5.4）。
 * <p>
 * 每分钟扫描 status=0 且 close_time < now 的订单：关单（0→5）+ 释放预占（0→2）+ 操作日志。
 * 预留扩展：后续可换 Redis 延迟队列，表结构不变。
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final StockReservationMapper stockReservationMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final StockClient stockClient;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void closeExpiredOrders() {
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .lt(Order::getCloseTime, LocalDateTime.now()));
        for (Order order : expired) {
            order.setStatus(5);
            order.setCancelReason("超时未支付，系统关闭");
            orderMapper.updateById(order);
            releaseReservations(order.getId());
            writeLog(order.getId(), "CLOSE", 0, 5, "超时未支付，系统关闭");
        }
    }

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

    private void writeLog(Long orderId, String action, Integer fromStatus, Integer toStatus, String remark) {
        OrderOperationLog log = new OrderOperationLog();
        log.setOrderId(orderId);
        log.setOperatorType(2);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderOperationLogMapper.insert(log);
    }
}