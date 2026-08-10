package com.chareslm.shopping.trade.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 DTO。
 * 状态机：0待支付/1已支付/2已发货/3已完成/4已取消/5已关闭/6退款中/7已退款。
 */
@Getter
@Setter
public class OrderDTO {

    private Long orderId;

    private String orderNo;

    private Integer status;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime closeTime;

    private LocalDateTime finishTime;

    private List<OrderItemDTO> items;
}