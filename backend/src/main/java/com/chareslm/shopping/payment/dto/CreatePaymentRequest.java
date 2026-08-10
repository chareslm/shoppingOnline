package com.chareslm.shopping.payment.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 创建支付单请求（一单一付）。
 */
@Getter
@Setter
public class CreatePaymentRequest {

    private Long orderId;
}