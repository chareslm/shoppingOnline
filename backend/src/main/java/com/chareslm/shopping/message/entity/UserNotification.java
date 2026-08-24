package com.chareslm.shopping.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户通知（站内信）。
 * category: 1系统/2订单/3营销/4客服
 */
@Getter
@Setter
@TableName("user_notification")
public class UserNotification extends BaseEntity {

    /** 接收用户ID */
    private Long userId;

    /** 关联模板ID */
    private Long templateId;

    /** 模板编码(冗余) */
    private String templateCode;

    /** 通知标题 */
    private String title;

    /** 通知内容(已渲染变量) */
    private String content;

    /** 分类: 1系统/2订单/3营销/4客服 */
    private Integer category;

    /** 关联业务类型(如 ORDER) */
    private String bizType;

    /** 关联业务ID */
    private String bizId;

    /** 已读标识: 0未读/1已读 */
    private Integer isRead;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 推送状态: 0未推送/1推送成功/2推送失败 */
    private Integer pushStatus;

    /** 推送时间 */
    private LocalDateTime pushTime;
}
