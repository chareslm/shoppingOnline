package com.chareslm.shopping.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息模板。
 * category: 1系统/2订单/3营销/4客服
 */
@Getter
@Setter
@TableName("message_template")
public class MessageTemplate extends BaseEntity {

    /** 模板编码(业务唯一, 如 ORDER_PAID) */
    private String templateCode;

    /** 模板标题 */
    private String title;

    /** 模板内容(支持变量替换, 如 {orderNo}) */
    private String content;

    /** 分类: 1系统/2订单/3营销/4客服 */
    private Integer category;

    /** 是否启用推送: 1启用/0不推送 */
    private Integer pushEnabled;

    /** 状态: 1启用/0禁用 */
    private Integer status;
}
