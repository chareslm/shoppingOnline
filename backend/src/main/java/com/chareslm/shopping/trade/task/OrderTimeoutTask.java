package com.chareslm.shopping.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
    public int closeExpiredOrders() {
        List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .lt(Order::getCloseTime, LocalDateTime.now()));
        int closed = 0;
        for (Order order : expired) {
            // 条件更新：仅当 status=0 时 0→5，与支付回调(0→1)/用户取消(0→4)并发时只有一个成功
            Order update = new Order();
            update.setStatus(5);
            update.setCancelReason("超时未支付，系统关闭");
            int rows = orderMapper.update(update, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, order.getId())
                    .eq(Order::getStatus, 0));
            if (rows == 0) {
                continue;
            }
            closed++;
            releaseReservations(order.getId());
            writeLog(order.getId(), "CLOSE", 0, 5, "超时未支付，系统关闭");
        }
        return closed;
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