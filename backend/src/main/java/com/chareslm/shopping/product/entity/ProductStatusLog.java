package com.chareslm.shopping.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商品状态流转日志（只追加审计）。
 */
@Getter
@Setter
@TableName("product_status_log")
public class ProductStatusLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 SPU */
    private Long spuId;

    /** 操作者 ID */
    private Long operatorId;

    /** SUBMIT/APPROVE/REJECT/PUBLISH/OFF_SHELF */
    private String action;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 备注 */
    private String remark;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
