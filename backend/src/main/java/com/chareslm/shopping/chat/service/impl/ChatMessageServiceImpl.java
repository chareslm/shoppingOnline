package com.chareslm.shopping.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chareslm.shopping.chat.controller.ChatWebSocketHandler;
import com.chareslm.shopping.chat.converter.ChatMessageConverter;
import com.chareslm.shopping.chat.dto.request.SendMessageRequest;
import com.chareslm.shopping.chat.dto.response.MessageResponse;
import com.chareslm.shopping.chat.entity.ChatMessage;
import com.chareslm.shopping.chat.entity.ChatSession;
import com.chareslm.shopping.chat.enums.MessageType;
import com.chareslm.shopping.chat.enums.SenderType;
import com.chareslm.shopping.chat.enums.SessionStatus;
import com.chareslm.shopping.chat.event.MessageSentEvent;
import com.chareslm.shopping.chat.mapper.ChatMessageMapper;
import com.chareslm.shopping.chat.mapper.ChatSessionMapper;
import com.chareslm.shopping.chat.service.ChatMessageService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.service.UserNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 聊天消息服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    /** 撤回时间窗口（分钟） */
    private static final int RECALL_WINDOW_MINUTES = 5;

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final UserNotificationService notificationService;
    private final ChatWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageResponse sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "sendMessage");
        MDC.put("sessionId", String.valueOf(sessionId));

        // 校验消息内容
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.MESSAGE_EMPTY);
        }

        ChatSession session = requireAccessibleSession(userId, sessionId);

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(userId);
        msg.setSenderType(determineSenderType(userId, session).getCode());
        msg.setContent(request.getContent());
        msg.setMsgType(request.getMsgType() != null ? request.getMsgType() : MessageType.TEXT.getCode());
        msg.setExtraData(request.getExtraData());
        msg.setIsRead(0);
        msg.setStatus(1);
        messageMapper.insert(msg);

        // 更新会话最后消息冗余字段
        session.setLastMessage(msg.getContent());
        session.setLastMessageTime(msg.getCreatedAt());
        sessionMapper.updateById(session);

        // 推送通知给对方
        Long receiverId = resolveReceiverId(userId, session);
        if (receiverId != null) {
            notificationService.sendChatNotification(receiverId, sessionId, msg.getContent());
        }

        // 预留：Redis 缓存未读数
        // redisTemplate.opsForHash().increment("chat:unread:" + sessionId, String.valueOf(receiverId != null ? receiverId : userId), 1);

        // 发布领域事件
        MessageResponse response = ChatMessageConverter.toResponse(msg);
        MessageSentEvent event = new MessageSentEvent(
                msg.getId(), sessionId, userId, receiverId,
                msg.getMsgType(), msg.getContent(), msg.getCreatedAt());
        eventPublisher.publishEvent(event);

        // WebSocket 实时推送给对方
        if (receiverId != null) {
            webSocketHandler.pushMessage(receiverId, response);
        }

        log.info("Message sent: messageId={}, sessionId={}, senderId={}", msg.getId(), sessionId, userId);
        MDC.clear();

        return response;
    }

    @Override
    public List<MessageResponse> listMessages(Long userId, Long sessionId, int page, int pageSize) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "listMessages");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = requireAccessibleSession(userId, sessionId);

        Page<ChatMessage> result = messageMapper.selectPage(
                Page.of(Math.max(page - 1, 0), pageSize),
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getStatus, 1)
                        .orderByAsc(ChatMessage::getCreatedAt));

        List<MessageResponse> responses = result.getRecords().stream()
                .map(ChatMessageConverter::toResponse).toList();
        log.info("Listed {} messages for sessionId={}", responses.size(), sessionId);
        MDC.clear();
        return responses;
    }

    @Override
    public List<MessageResponse> pullOfflineMessages(Long userId, Long sessionId, Long lastMessageId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "pullOfflineMessages");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = requireAccessibleSession(userId, sessionId);

        List<ChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getStatus, 1)
                .gt(lastMessageId != null, ChatMessage::getId, lastMessageId)
                .orderByAsc(ChatMessage::getCreatedAt));

        List<MessageResponse> responses = messages.stream()
                .map(ChatMessageConverter::toResponse).toList();
        log.info("Pulled {} offline messages for sessionId={}", responses.size(), sessionId);
        MDC.clear();
        return responses;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long sessionId, List<Long> messageIds) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "markAsRead");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = requireAccessibleSession(userId, sessionId);
        Long otherPartyId = resolveReceiverId(userId, session);

        LocalDateTime now = LocalDateTime.now();
        int marked = 0;
        for (Long msgId : messageIds) {
            ChatMessage msg = messageMapper.selectById(msgId);
            if (msg != null && msg.getSessionId().equals(sessionId)
                    && msg.getSenderId().equals(otherPartyId)
                    && msg.getIsRead() == 0) {
                msg.setIsRead(1);
                msg.setReadTime(now);
                messageMapper.updateById(msg);
                marked++;
            }
        }

        // 预留：Redis 清除未读缓存
        // redisTemplate.opsForHash().delete("chat:unread:" + sessionId, String.valueOf(userId));

        log.info("Marked {} messages as read for sessionId={}, userId={}", marked, sessionId, userId);
        MDC.clear();
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "recallMessage");
        MDC.put("messageId", String.valueOf(messageId));

        ChatMessage msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!msg.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        long minutesBetween = ChronoUnit.MINUTES.between(msg.getCreatedAt(), LocalDateTime.now());
        if (minutesBetween > RECALL_WINDOW_MINUTES) {
            throw new BusinessException(ErrorCode.MESSAGE_RECALL_EXPIRED);
        }
        msg.setStatus(0);
        messageMapper.updateById(msg);

        log.info("Message recalled: messageId={}, userId={}", messageId, userId);
        MDC.clear();
    }

    private ChatSession requireAccessibleSession(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!session.getUserId().equals(userId) && !userId.equals(session.getCsUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private SenderType determineSenderType(Long userId, ChatSession session) {
        if (session.getCsUserId() != null && session.getCsUserId().equals(userId)) {
            return SenderType.CUSTOMER_SERVICE;
        }
        return SenderType.USER;
    }

    private Long resolveReceiverId(Long senderId, ChatSession session) {
        if (session.getUserId().equals(senderId)) {
            return session.getCsUserId();
        }
        return session.getUserId();
    }
}
