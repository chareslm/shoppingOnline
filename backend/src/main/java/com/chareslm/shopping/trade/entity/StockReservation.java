package com.chareslm.shopping.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 库存预占记录（下单预占 + 超时释放核心表）。
 * <p>
 * 实际库存扣减由服务层调用成员3库存接口完成，本表记录预占生命周期。
 */
@Getter
@Setter
@TableName("stock_reservation")
public class StockReservation extends BaseEntity {

    /** 关联订单 */
    private Long orderId;

    /** SKU ID */
    private Long skuId;

    /** 预占数量 */
    private Integer quantity;

    /** 0 预占中 / 1 已扣减 / 2 已释放 */
    private Integer status;

    /** 预占过期时间（= 订单支付超时时间） */
    private LocalDateTime expireTime;
}