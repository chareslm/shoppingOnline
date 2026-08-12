package com.chareslm.shopping.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.payment.dto.CreatePaymentRequest;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.dto.RefundOrderDTO;
import com.chareslm.shopping.payment.dto.RefundRequest;
import com.chareslm.shopping.payment.entity.PaymentOrder;
import com.chareslm.shopping.payment.entity.PaymentRecord;
import com.chareslm.shopping.payment.entity.RefundOrder;
import com.chareslm.shopping.payment.mapper.PaymentOrderMapper;
import com.chareslm.shopping.payment.mapper.PaymentRecordMapper;
import com.chareslm.shopping.payment.mapper.RefundOrderMapper;
import com.chareslm.shopping.payment.service.PaymentService;
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.OrderOperationLog;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.OrderOperationLogMapper;
import com.chareslm.shopping.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现。
 * <p>
 * 幂等策略（设计文档 §5.2）：payment_record 唯一约束 (payment_order_id, callback_type, status)
 * 保证并发回调下只有一条 status=1 处理记录；插入冲突（DuplicateKeyException）说明已处理，
 * 改插 status=2 重复记录后直接返回。所有状态变更使用条件更新（WHERE status=?）防并发覆盖。
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String CHANNEL = "MOCK_WECHAT";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final OrderService orderService;

    @Override
    @Transactional
    public PaymentOrderDTO createPaymentOrder(Long userId, CreatePaymentRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPaymentNo(generateNo("PAY"));
        paymentOrder.setOrderId(order.getId());
        paymentOrder.setUserId(userId);
        paymentOrder.setAmount(order.getPayAmount());
        paymentOrder.setStatus(0);
        paymentOrder.setPayChannel(CHANNEL);
        paymentOrder.setOutTradeNo(paymentOrder.getPaymentNo());
        paymentOrder.setExpireTime(order.getCloseTime());
        paymentOrderMapper.insert(paymentOrder);
        return toDTO(paymentOrder);
    }

    @Override
    @Transactional
    public PaymentOrderDTO mockPay(Long userId, Long paymentOrderId) {
        PaymentOrder paymentOrder = requireOwnedPaymentOrder(userId, paymentOrderId);
        // 条件更新：仅当 status=0 时置 transactionId/prepayId，并发下只有一个线程成功
        PaymentOrder update = new PaymentOrder();
        update.setTransactionId(generateNo("TXN"));
        update.setPrepayId(generateNo("PRE"));
        int rows = paymentOrderMapper.update(update, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, paymentOrderId)
                .eq(PaymentOrder::getStatus, 0));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        handlePayCallback(paymentOrderId, "{\"mock\":true,\"transactionId\":\"" + update.getTransactionId() + "\"}");
        return toDTO(paymentOrderMapper.selectById(paymentOrderId));
    }

    @Override
    @Transactional
    public void handlePayCallback(Long paymentOrderId, String rawData) {
        // Serialize concurrent callbacks: FOR UPDATE locks the payment order row so
        // concurrent callbacks queue up instead of racing on the unique key insert
        // (which caused InnoDB deadlocks in markDuplicateCallback).
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, paymentOrderId)
                .last("FOR UPDATE"));
        if (paymentOrder == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // Idempotent: already paid (status=1) -> record duplicate and return.
        if (paymentOrder.getStatus() == 1) {
            markDuplicateCallback(paymentOrderId, rawData);
            return;
        }
        // Idempotent: insert status=1 processing record; unique constraint
        // (payment_order_id, callback_type, status) is the fallback guard.
        PaymentRecord record = new PaymentRecord();
        record.setPaymentOrderId(paymentOrderId);
        record.setCallbackType("PAY");
        record.setRawData(rawData);
        record.setStatus(1);
        record.setProcessResult("支付成功");
        try {
            paymentRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            markDuplicateCallback(paymentOrderId, rawData);
            return;
        }
        // 条件更新：仅当 status=0 时 0→1，防止与超时关闭/取消并发覆盖
        PaymentOrder update = new PaymentOrder();
        update.setStatus(1);
        update.setPayTime(LocalDateTime.now());
        update.setCallbackTime(LocalDateTime.now());
        update.setCallbackRaw(rawData);
        int rows = paymentOrderMapper.update(update, new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getId, paymentOrderId)
                .eq(PaymentOrder::getStatus, 0));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        orderService.markPaid(paymentOrder.getOrderId());
    }

    @Override
    @Transactional
    public void refund(Long userId, RefundRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        // 悲观锁（FOR UPDATE）串行化退款申请：并发退款时第二个线程等待并读到最新数据，
        // 结合"已退 + 待退"累计校验，防止并发超退
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, order.getId())
                .last("FOR UPDATE"));
        if (paymentOrder == null || paymentOrder.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        BigDecimal refundAmount = request.getAmount() == null ? order.getPayAmount() : request.getAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        // 累计校验：已退金额 + 待退金额 + 本次退款不得超过实付金额（防并发超退）
        BigDecimal refunded = sumRefunded(paymentOrder.getId());
        BigDecimal pending = sumPending(paymentOrder.getId());
        if (refundAmount.add(refunded).add(pending).compareTo(order.getPayAmount()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(generateNo("REF"));
        refundOrder.setPaymentOrderId(paymentOrder.getId());
        refundOrder.setOrderId(order.getId());
        refundOrder.setUserId(userId);
        refundOrder.setAmount(refundAmount);
        refundOrder.setReason(request.getReason());
        refundOrder.setStatus(0);
        refundOrderMapper.insert(refundOrder);

        // 条件更新：仅当 status IN (1,2) 时 1/2→6（退款中），防并发覆盖
        Order update = new Order();
        update.setStatus(6);
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .in(Order::getStatus, 1, 2));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        writeOrderLog(order.getId(), 1, userId, "REFUND", order.getStatus(), 6, "申请退款");
    }

    @Override
    @Transactional
    public void completeRefund(Long refundId) {
        RefundOrder refundOrder = refundOrderMapper.selectById(refundId);
        if (refundOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 条件更新：仅当 status=0 时 0→1，防并发重复完成
        RefundOrder update = new RefundOrder();
        update.setStatus(1);
        update.setChannelRefundId(generateNo("CHREF"));
        update.setRefundTime(LocalDateTime.now());
        int rows = refundOrderMapper.update(update, new LambdaUpdateWrapper<RefundOrder>()
                .eq(RefundOrder::getId, refundId)
                .eq(RefundOrder::getStatus, 0));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID);
        }

        PaymentOrder paymentOrder = paymentOrderMapper.selectById(refundOrder.getPaymentOrderId());
        if (paymentOrder == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // 累计已退金额达到实付金额 → 全额退完：订单 6→7 + 支付单 1→4（已退款）
        // 否则为部分退款：订单保持 6（退款中），支付单保持 1
        BigDecimal refunded = sumRefunded(paymentOrder.getId());
        if (refunded.compareTo(paymentOrder.getAmount()) >= 0) {
            Order orderUpdate = new Order();
            orderUpdate.setStatus(7);
            int orderRows = orderMapper.update(orderUpdate, new LambdaUpdateWrapper<Order>()
                    .eq(Order::getId, refundOrder.getOrderId())
                    .eq(Order::getStatus, 6));
            if (orderRows > 0) {
                writeOrderLog(refundOrder.getOrderId(), 2, null, "REFUND", 6, 7, "退款成功");
            }
            PaymentOrder poUpdate = new PaymentOrder();
            poUpdate.setStatus(4);
            paymentOrderMapper.update(poUpdate, new LambdaUpdateWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getId, paymentOrder.getId())
                    .eq(PaymentOrder::getStatus, 1));
        }
    }

    @Override
    public PaymentOrderDTO getPaymentOrder(Long userId, Long paymentOrderId) {
        return toDTO(requireOwnedPaymentOrder(userId, paymentOrderId));
    }

    @Override
    public List<RefundOrderDTO> listRefunds(Long userId) {
        List<RefundOrder> refunds = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getUserId, userId)
                .orderByDesc(RefundOrder::getCreatedAt));
        return refunds.stream().map(this::toRefundDTO).toList();
    }

    /**
     * 统计支付单已成功退款的累计金额（refund_order.status=1）。
     */
    private BigDecimal sumRefunded(Long paymentOrderId) {
        List<RefundOrder> refundedOrders = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getPaymentOrderId, paymentOrderId)
                .eq(RefundOrder::getStatus, 1));
        return refundedOrders.stream()
                .map(RefundOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 统计支付单待处理退款的累计金额（refund_order.status=0）。
     */
    private BigDecimal sumPending(Long paymentOrderId) {
        List<RefundOrder> pendingOrders = refundOrderMapper.selectList(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getPaymentOrderId, paymentOrderId)
                .eq(RefundOrder::getStatus, 0));
        return pendingOrders.stream()
                .map(RefundOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 标记重复回调（status=2）。唯一约束冲突（重复记录已存在）时静默忽略。
     */
    private void markDuplicateCallback(Long paymentOrderId, String rawData) {
        try {
            PaymentRecord duplicate = new PaymentRecord();
            duplicate.setPaymentOrderId(paymentOrderId);
            duplicate.setCallbackType("PAY");
            duplicate.setRawData(rawData);
            duplicate.setStatus(2);
            duplicate.setProcessResult("重复回调，已忽略");
            paymentRecordMapper.insert(duplicate);
        } catch (DuplicateKeyException ignored) {
            // 重复记录已存在，无需再写
        }
    }

    private PaymentOrder requireOwnedPaymentOrder(Long userId, Long paymentOrderId) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentOrderId);
        if (paymentOrder == null || !paymentOrder.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        return paymentOrder;
    }

    private void writeOrderLog(Long orderId, int operatorType, Long operatorId, String action,
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

    private RefundOrderDTO toRefundDTO(RefundOrder refundOrder) {
        RefundOrderDTO dto = new RefundOrderDTO();
        dto.setRefundId(refundOrder.getId());
        dto.setRefundNo(refundOrder.getRefundNo());
        dto.setPaymentOrderId(refundOrder.getPaymentOrderId());
        dto.setOrderId(refundOrder.getOrderId());
        dto.setAmount(refundOrder.getAmount());
        dto.setReason(refundOrder.getReason());
        dto.setStatus(refundOrder.getStatus());
        dto.setRefundTime(refundOrder.getRefundTime());
        dto.setCreatedAt(refundOrder.getCreatedAt());
        return dto;
    }

    private PaymentOrderDTO toDTO(PaymentOrder paymentOrder) {
        PaymentOrderDTO dto = new PaymentOrderDTO();
        dto.setPaymentOrderId(paymentOrder.getId());
        dto.setPaymentNo(paymentOrder.getPaymentNo());
        dto.setOrderId(paymentOrder.getOrderId());
        dto.setUserId(paymentOrder.getUserId());
        dto.setAmount(paymentOrder.getAmount());
        dto.setStatus(paymentOrder.getStatus());
        dto.setPayChannel(paymentOrder.getPayChannel());
        dto.setPayTime(paymentOrder.getPayTime());
        dto.setExpireTime(paymentOrder.getExpireTime());
        return dto;
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}