package com.chareslm.shopping.trade.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 下单请求：结算当前用户全部勾选购物项，按商家拆单。
 */
@Getter
@Setter
public class CreateOrderRequest {

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;
}