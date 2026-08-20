package com.chareslm.shopping.chat.service.impl;

import com.chareslm.shopping.chat.controller.ChatWebSocketHandler;
import com.chareslm.shopping.chat.dto.request.SendMessageRequest;
import com.chareslm.shopping.chat.dto.response.MessageResponse;
import com.chareslm.shopping.chat.entity.ChatMessage;
import com.chareslm.shopping.chat.entity.ChatSession;
import com.chareslm.shopping.chat.enums.MessageType;
import com.chareslm.shopping.chat.enums.SenderType;
import com.chareslm.shopping.chat.mapper.ChatMessageMapper;
import com.chareslm.shopping.chat.mapper.ChatSessionMapper;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.service.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 聊天消息服务单元测试。
 */
class ChatMessageServiceImplTest {

    private ChatMessageMapper messageMapper;
    private ChatSessionMapper sessionMapper;
    private UserNotificationService notificationService;
    private ChatWebSocketHandler webSocketHandler;
    private ApplicationEventPublisher eventPublisher;
    private ChatMessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        messageMapper = mock(ChatMessageMapper.class);
        sessionMapper = mock(ChatSessionMapper.class);
        notificationService = mock(UserNotificationService.class);
        webSocketHandler = mock(ChatWebSocketHandler.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        messageService = new ChatMessageServiceImpl(messageMapper, sessionMapper, notificationService,
                webSocketHandler, eventPublisher);
    }

    @Test
    void sendMessagePersistsMessageAndUpdatesSession() {
        // given: 用户向客服发送消息
        Long userId = 1L;
        Long csUserId = 2L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setCsUserId(csUserId);

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("你好，我想咨询商品");
        request.setMsgType(MessageType.TEXT.getCode());

        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(100L);
            msg.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        // when
        MessageResponse response = messageService.sendMessage(userId, sessionId, request);

        // then: 消息被持久化
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(sessionId, response.getSessionId());
        assertEquals(userId, response.getSenderId());
        assertEquals(SenderType.USER.getCode(), response.getSenderType());
        assertEquals("你好，我想咨询商品", response.getContent());

        verify(messageMapper).insert(any(ChatMessage.class));
        verify(sessionMapper).updateById(any(ChatSession.class)); // 更新会话最后消息
        verify(notificationService).sendChatNotification(eq(csUserId), eq(sessionId), any());
    }

    @Test
    void sendMessageRejectsEmptyContent() {
        // given: 空消息内容
        Long userId = 1L;
        Long sessionId = 10L;

        SendMessageRequest request = new SendMessageRequest();
        request.setContent(""); // 空内容

        // when & then
        assertThrows(BusinessException.class,
                () -> messageService.sendMessage(userId, sessionId, request));

        verify(messageMapper, never()).insert(any(ChatMessage.class));
    }

    @Test
    void sendMessageRejectsUnauthorizedSessionAccess() {
        // given: 用户尝试向不属于自己的会话发消息
        Long hackerUserId = 999L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(1L); // 属于其他用户
        session.setCsUserId(2L);

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("测试");

        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        // when & then
        assertThrows(BusinessException.class,
                () -> messageService.sendMessage(hackerUserId, sessionId, request));

        verify(messageMapper, never()).insert(any(ChatMessage.class));
    }

    @Test
    void markAsReadUpdatesReadStatus() {
        // given: 标记对方消息为已读
        Long userId = 1L;
        Long csUserId = 2L;
        Long sessionId = 10L;
        Long messageId = 100L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setCsUserId(csUserId);

        ChatMessage msg = new ChatMessage();
        msg.setId(messageId);
        msg.setSessionId(sessionId);
        msg.setSenderId(csUserId); // 对方是客服
        msg.setIsRead(0);

        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.selectById(messageId)).thenReturn(msg);

        // when
        messageService.markAsRead(userId, sessionId, List.of(messageId));

        // then: 已读状态被更新
        verify(messageMapper).updateById(any(ChatMessage.class));
    }

    @Test
    void markAsReadSkipsOwnMessages() {
        // given: 用户标记自己发的消息为已读（应该被跳过）
        Long userId = 1L;
        Long csUserId = 2L;
        Long sessionId = 10L;
        Long ownMessageId = 101L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setCsUserId(csUserId);

        ChatMessage ownMsg = new ChatMessage();
        ownMsg.setId(ownMessageId);
        ownMsg.setSessionId(sessionId);
        ownMsg.setSenderId(userId); // 自己发的消息

        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.selectById(ownMessageId)).thenReturn(ownMsg);

        // when
        messageService.markAsRead(userId, sessionId, List.of(ownMessageId));

        // then: 不应更新已读状态（不是对方消息）
        verify(messageMapper, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void recallMessageWithinWindowSucceeds() {
        // given: 5分钟内的消息可以撤回
        Long userId = 1L;
        Long messageId = 100L;

        ChatMessage msg = new ChatMessage();
        msg.setId(messageId);
        msg.setSenderId(userId);
        msg.setStatus(1);
        msg.setCreatedAt(LocalDateTime.now().minusMinutes(2)); // 2分钟前

        when(messageMapper.selectById(messageId)).thenReturn(msg);

        // when
        messageService.recallMessage(userId, messageId);

        // then: 状态被更新为已撤回
        verify(messageMapper).updateById(any(ChatMessage.class));
    }

    @Test
    void recallMessageExpiredThrowsException() {
        // given: 超过5分钟的消息不可撤回
        Long userId = 1L;
        Long messageId = 100L;

        ChatMessage msg = new ChatMessage();
        msg.setId(messageId);
        msg.setSenderId(userId);
        msg.setStatus(1);
        msg.setCreatedAt(LocalDateTime.now().minusMinutes(10)); // 10分钟前

        when(messageMapper.selectById(messageId)).thenReturn(msg);

        // when & then: 撤回过期
        assertThrows(BusinessException.class,
                () -> messageService.recallMessage(userId, messageId));

        verify(messageMapper, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void recallMessageByNonSenderRejected() {
        // given: 非消息发送者尝试撤回
        Long senderId = 1L;
        Long otherUserId = 999L;
        Long messageId = 100L;

        ChatMessage msg = new ChatMessage();
        msg.setId(messageId);
        msg.setSenderId(senderId); // 属于其他人
        msg.setStatus(1);
        msg.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(messageMapper.selectById(messageId)).thenReturn(msg);

        // when & then: 只有发送者才能撤回
        assertThrows(BusinessException.class,
                () -> messageService.recallMessage(otherUserId, messageId));

        verify(messageMapper, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void pullOfflineMessagesReturnsMessagesAfterLastId() {
        // given: 拉取离线消息
        Long userId = 1L;
        Long sessionId = 10L;
        Long lastMessageId = 50L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setCsUserId(2L);

        ChatMessage newMsg = new ChatMessage();
        newMsg.setId(100L);
        newMsg.setSessionId(sessionId);
        newMsg.setSenderId(2L);
        newMsg.setContent("离线消息");
        newMsg.setStatus(1);
        newMsg.setIsRead(0);
        newMsg.setCreatedAt(LocalDateTime.now());

        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.selectList(any())).thenReturn(List.of(newMsg));

        // when
        List<MessageResponse> result = messageService.pullOfflineMessages(userId, sessionId, lastMessageId);

        // then
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
        assertEquals("离线消息", result.get(0).getContent());
    }
}
