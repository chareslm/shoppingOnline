package com.chareslm.shopping.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（按商家拆单：一个订单只属于一个店铺）。
 * <p>
 * 状态机：0待支付→1已支付→2已发货→3已完成；0→4已取消；0→5已关闭（超时）；1/2→6退款中→7已退款。
 * 注意：{@code order} 是 MySQL 保留字，表名必须带反引号。
 */
@Getter
@Setter
@TableName("`order`")
public class Order extends BaseEntity {

    /** 业务订单号 */
    private String orderNo;

    /** 下单用户（引用成员1 user 表） */
    private Long userId;

    /** 店铺 ID（按商家拆单） */
    private Long shopId;

    /** 状态机：0待支付/1已支付/2已发货/3已完成/4已取消/5已关闭/6退款中/7已退款 */
    private Integer status;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 = total + freight - discount */
    private BigDecimal payAmount;

    /** 收货人快照 */
    private String receiverName;

    /** 收货电话快照 */
    private String receiverPhone;

    /** 收货地址快照 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 超时关闭时间 */
    private LocalDateTime closeTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消原因 */
    private String cancelReason;
}