package com.chareslm.shopping.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 订单操作日志（审计：高风险操作必须留痕）。
 * <p>
 * 仅含 created_at，不继承 BaseEntity。
 */
@Getter
@Setter
@TableName("order_operation_log")
public class OrderOperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联订单 */
    private Long orderId;

    /** 1 用户 / 2 系统 / 3 管理员 */
    private Integer operatorType;

    /** 操作者 ID */
    private Long operatorId;

    /** CREATE / PAY / CANCEL / CLOSE / SHIP / COMPLETE / REFUND */
    private String action;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 备注 */
    private String remark;

    /** 操作时间 */
    private LocalDateTime createdAt;
}