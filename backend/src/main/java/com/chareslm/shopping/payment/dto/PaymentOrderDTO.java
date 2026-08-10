package com.chareslm.shopping.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 DTO。
 * 状态：0待支付/1成功/2失败/3已关闭/4已退款。
 */
@Getter
@Setter
public class PaymentOrderDTO {

    private Long paymentOrderId;

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private Integer status;

    private String payChannel;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;
}