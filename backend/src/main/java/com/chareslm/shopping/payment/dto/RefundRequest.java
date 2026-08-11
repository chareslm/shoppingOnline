package com.chareslm.shopping.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 退款请求。
 */
@Getter
@Setter
public class RefundRequest {

    private Long orderId;

    private BigDecimal amount;

    private String reason;
}