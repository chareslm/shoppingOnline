package com.chareslm.shopping.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客服会话：用户与客服的一次对话会话。
 * 状态: 0进行中/1已结束
 */
@Getter
@Setter
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    /** 发起用户ID */
    private Long userId;

    /** 所属商家ID（可为空=平台客服） */
    private Long shopId;

    /** 分配的客服用户ID */
    private Long csUserId;

    /** 会话主题/关联订单号 */
    private String subject;

    /** 最后一条消息内容(冗余) */
    private String lastMessage;

    /** 最后消息时间(冗余) */
    private LocalDateTime lastMessageTime;

    /** 状态: 0进行中/1已结束 */
    private Integer status;

    /** 优先级: 0普通/1优先 */
    private Integer priority;
}