package com.chareslm.shopping.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户通知偏好。
 */
@Getter
@Setter
@TableName("notification_preference")
public class NotificationPreference extends BaseEntity {

    /** 用户ID */
    private Long userId;

    /** 系统通知开关 */
    private Integer systemEnabled;

    /** 订单通知开关 */
    private Integer orderEnabled;

    /** 营销通知开关 */
    private Integer marketingEnabled;

    /** 客服消息开关 */
    private Integer serviceEnabled;
}
