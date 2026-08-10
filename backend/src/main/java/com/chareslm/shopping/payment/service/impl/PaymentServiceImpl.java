package com.chareslm.shopping.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.common.api.ErrorCode;
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
import com.chareslm.shopping.trade.entity.Order;
import com.chareslm.shopping.trade.entity.OrderOperationLog;
import com.chareslm.shopping.trade.mapper.OrderMapper;
import com.chareslm.shopping.trade.mapper.OrderOperationLogMapper;
import com.chareslm.shopping.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现。
 * <p>
 * 幂等策略（设计文档 §5.2）：回调先查 payment_record 已处理记录，存在则标记重复直接返回；
 * 否则同一事务内写 payment_record + 更新 payment_order.status=1 + 订单 markPaid + 操作日志。
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
        if (paymentOrder.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        paymentOrder.setTransactionId(generateNo("TXN"));
        paymentOrder.setPrepayId(generateNo("PRE"));
        paymentOrderMapper.updateById(paymentOrder);
        handlePayCallback(paymentOrderId, "{\"mock\":true,\"transactionId\":\"" + paymentOrder.getTransactionId() + "\"}");
        return toDTO(paymentOrderMapper.selectById(paymentOrderId));
    }

    @Override
    @Transactional
    public void handlePayCallback(Long paymentOrderId, String rawData) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentOrderId);
        if (paymentOrder == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        // 幂等：已处理直接返回（标记重复）
        PaymentRecord existing = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentOrderId, paymentOrderId)
                .eq(PaymentRecord::getCallbackType, "PAY")
                .eq(PaymentRecord::getStatus, 1));
        if (existing != null) {
            PaymentRecord duplicate = new PaymentRecord();
            duplicate.setPaymentOrderId(paymentOrderId);
            duplicate.setCallbackType("PAY");
            duplicate.setRawData(rawData);
            duplicate.setStatus(2);
            duplicate.setProcessResult("重复回调，已忽略");
            paymentRecordMapper.insert(duplicate);
            return;
        }
        if (paymentOrder.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        // 同一事务：写回调记录 + 支付单成功 + 订单已支付 + 操作日志
        PaymentRecord record = new PaymentRecord();
        record.setPaymentOrderId(paymentOrderId);
        record.setCallbackType("PAY");
        record.setRawData(rawData);
        record.setStatus(1);
        record.setProcessResult("支付成功");
        paymentRecordMapper.insert(record);

        paymentOrder.setStatus(1);
        paymentOrder.setPayTime(LocalDateTime.now());
        paymentOrder.setCallbackTime(LocalDateTime.now());
        paymentOrder.setCallbackRaw(rawData);
        paymentOrderMapper.updateById(paymentOrder);

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
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, order.getId()));
        if (paymentOrder == null || paymentOrder.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        BigDecimal refundAmount = request.getAmount() == null ? order.getPayAmount() : request.getAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(order.getPayAmount()) > 0) {
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

        int fromStatus = order.getStatus();
        order.setStatus(6);
        orderMapper.updateById(order);
        writeOrderLog(order.getId(), 1, userId, "REFUND", fromStatus, 6, "申请退款");
    }

    @Override
    @Transactional
    public void completeRefund(Long refundId) {
        RefundOrder refundOrder = refundOrderMapper.selectById(refundId);
        if (refundOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (refundOrder.getStatus() != 0) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID);
        }
        refundOrder.setStatus(1);
        refundOrder.setChannelRefundId(generateNo("CHREF"));
        refundOrder.setRefundTime(LocalDateTime.now());
        refundOrderMapper.updateById(refundOrder);

        Order order = orderMapper.selectById(refundOrder.getOrderId());
        if (order != null && order.getStatus() == 6) {
            order.setStatus(7);
            orderMapper.updateById(order);
            writeOrderLog(order.getId(), 2, null, "REFUND", 6, 7, "退款成功");
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