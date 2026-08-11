package com.chareslm.shopping.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录（按日对账）。
 * <p>
 * 含 created_at/updated_at 但无 version，不继承 BaseEntity。
 */
@Getter
@Setter
@TableName("reconciliation_record")
public class ReconciliationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对账日期 */
    private LocalDate bizDate;

    /** 渠道 */
    private String channel;

    /** 渠道侧总金额 */
    private BigDecimal totalAmount;

    /** 渠道侧总笔数 */
    private Integer totalCount;

    /** 差异笔数 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 0 待对账 / 1 一致 / 2 有差异 */
    private Integer status;

    /** 差异明细（JSON） */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}