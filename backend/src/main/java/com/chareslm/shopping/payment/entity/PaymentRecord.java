package com.chareslm.shopping.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 支付回调记录（幂等：同一支付单同一回调类型只处理一次）。
 * <p>
 * 仅含 created_at，不继承 BaseEntity。
 */
@Getter
@Setter
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联支付单 */
    private Long paymentOrderId;

    /** PAY / REFUND */
    private String callbackType;

    /** 回调原始数据 */
    private String rawData;

    /** 0 待处理 / 1 已处理 / 2 重复 */
    private Integer status;

    /** 处理结果说明 */
    private String processResult;

    /** 回调时间 */
    private LocalDateTime createdAt;
}