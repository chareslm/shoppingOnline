package com.chareslm.shopping.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单 DTO。
 * 状态：0 待处理 / 1 已退款 / 2 失败 / 3 已拒绝。
 */
@Getter
@Setter
public class RefundOrderDTO {

    private Long refundId;

    private String refundNo;

    private Long paymentOrderId;

    private Long orderId;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private LocalDateTime refundTime;

    private LocalDateTime createdAt;
}