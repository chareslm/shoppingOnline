package com.chareslm.shopping.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天消息。
 */
@Getter
@Setter
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    /** 所属会话ID */
    @TableField("session_id")
    private Long sessionId;

    /** 发送方用户ID */
    @TableField("sender_id")
    private Long senderId;

    /** 发送方类型: 1用户/2客服/3系统 */
    @TableField("sender_type")
    private Integer senderType;

    /** 消息内容 */
    private String content;

    /** 消息类型: 1文本/2图片/3商品卡片/4系统通知 */
    @TableField("msg_type")
    private Integer msgType;

    /** 扩展数据(如商品卡片JSON) */
    @TableField("extra_data")
    private String extraData;

    /** 是否已读: 0未读/1已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 已读时间 */
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 状态: 1正常/0已撤回 */
    private Integer status;
}
