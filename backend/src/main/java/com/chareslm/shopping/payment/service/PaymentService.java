package com.chareslm.shopping.payment.service;

import com.chareslm.shopping.payment.dto.CreatePaymentRequest;
import com.chareslm.shopping.payment.dto.PaymentOrderDTO;
import com.chareslm.shopping.payment.dto.RefundOrderDTO;
import com.chareslm.shopping.payment.dto.RefundRequest;

import java.util.List;

/**
 * 支付服务：创建支付单、模拟支付、幂等回调、退款。
 * <p>
 * 模拟支付（MOCK_WECHAT），预留微信字段；回调幂等由 payment_record 查重保证。
 */
public interface PaymentService {

    /**
     * 创建支付单（一单一付，payment_no 唯一）。
     */
    PaymentOrderDTO createPaymentOrder(Long userId, CreatePaymentRequest request);

    /**
     * 模拟支付成功（生成 transaction_id 并触发回调处理）。
     */
    PaymentOrderDTO mockPay(Long userId, Long paymentOrderId);

    /**
     * 支付回调处理（幂等：同一支付单同一回调类型只处理一次）。
     */
    void handlePayCallback(Long paymentOrderId, String rawData);

    /**
     * 申请退款：创建退款单，订单 1/2→6（退款中）。
     */
    void refund(Long userId, RefundRequest request);

    /**
     * 模拟退款成功（系统操作）：退款单 0→1，订单 6→7（已退款）。
     */
    void completeRefund(Long refundId);

    /**
     * 查询支付单（校验归属当前用户）。
     */
    PaymentOrderDTO getPaymentOrder(Long userId, Long paymentOrderId);

    /**
     * 查询当前用户的退款单列表（按创建时间倒序）。
     */
    List<RefundOrderDTO> listRefunds(Long userId);
}