package com.chareslm.shopping.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单（一单一付）。
 * <p>
 * 模拟支付（MOCK_WECHAT），预留微信支付字段。
 */
@Getter
@Setter
@TableName("payment_order")
public class PaymentOrder extends BaseEntity {

    /** 支付单号（幂等键） */
    private String paymentNo;

    /** 关联订单（一单一付） */
    private Long orderId;

    /** 支付用户（引用成员1 user 表） */
    private Long userId;

    /** 支付金额 */
    private BigDecimal amount;

    /** 0 待支付 / 1 成功 / 2 失败 / 3 已关闭 / 4 已退款 */
    private Integer status;

    /** 支付渠道（模拟微信） */
    private String payChannel;

    /** 微信商户订单号（模拟 = payment_no，预留） */
    private String outTradeNo;

    /** 微信支付单号（模拟生成，预留） */
    private String transactionId;

    /** 微信预支付 ID（预留） */
    private String prepayId;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 支付超时时间 */
    private LocalDateTime expireTime;

    /** 回调到达时间 */
    private LocalDateTime callbackTime;

    /** 回调原始报文 */
    private String callbackRaw;
}