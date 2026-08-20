package com.chareslm.shopping.chat.service;

import com.chareslm.shopping.chat.dto.request.SendMessageRequest;
import com.chareslm.shopping.chat.dto.response.MessageResponse;

import java.util.List;

/**
 * 聊天消息服务接口。
 */
public interface ChatMessageService {

    /**
     * 发送消息。
     */
    MessageResponse sendMessage(Long userId, Long sessionId, SendMessageRequest request);

    /**
     * 列表消息。
     */
    List<MessageResponse> listMessages(Long userId, Long sessionId, int page, int pageSize);

    /**
     * 拉取离线消息。
     */
    List<MessageResponse> pullOfflineMessages(Long userId, Long sessionId, Long lastMessageId);

    /**
     * 标记已读。
     */
    void markAsRead(Long userId, Long sessionId, List<Long> messageIds);

    /**
     * 撤回消息。
     */
    void recallMessage(Long userId, Long messageId);
}
