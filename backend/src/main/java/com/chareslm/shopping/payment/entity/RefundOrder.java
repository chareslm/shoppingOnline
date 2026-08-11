package com.chareslm.shopping.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单。
 */
@Getter
@Setter
@TableName("refund_order")
public class RefundOrder extends BaseEntity {

    /** 退款单号 */
    private String refundNo;

    /** 关联支付单 */
    private Long paymentOrderId;

    /** 关联订单 */
    private Long orderId;

    /** 退款用户 */
    private Long userId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /** 0 待处理 / 1 已退款 / 2 失败 / 3 已拒绝 */
    private Integer status;

    /** 渠道退款单号（预留） */
    private String channelRefundId;

    /** 退款完成时间 */
    private LocalDateTime refundTime;
}