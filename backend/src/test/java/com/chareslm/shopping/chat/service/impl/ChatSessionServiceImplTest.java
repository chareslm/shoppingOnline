package com.chareslm.shopping.chat.service.impl;

import com.chareslm.shopping.chat.dto.request.CreateSessionRequest;
import com.chareslm.shopping.chat.dto.response.SessionResponse;
import com.chareslm.shopping.chat.entity.ChatMessage;
import com.chareslm.shopping.chat.entity.ChatSession;
import com.chareslm.shopping.chat.enums.MessageType;
import com.chareslm.shopping.chat.enums.SenderType;
import com.chareslm.shopping.chat.enums.SessionStatus;
import com.chareslm.shopping.chat.mapper.ChatMessageMapper;
import com.chareslm.shopping.chat.mapper.ChatSessionMapper;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客服会话服务单元测试。
 */
class ChatSessionServiceImplTest {

    private ChatSessionMapper sessionMapper;
    private ChatMessageMapper messageMapper;
    private MerchantShopQueryService merchantShopQueryService;
    private ChatSessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(ChatSessionMapper.class);
        messageMapper = mock(ChatMessageMapper.class);
        merchantShopQueryService = mock(MerchantShopQueryService.class);
        when(merchantShopQueryService.requireOpenShopById(anyLong()))
                .thenAnswer(invocation -> openShop(invocation.getArgument(0)));
        when(merchantShopQueryService.requireOpenStaffShop(anyLong())).thenReturn(openShop(100L));
        sessionService = new ChatSessionServiceImpl(sessionMapper, messageMapper, merchantShopQueryService);
    }

    @Test
    void createSessionCreatesNewSessionWithFirstMessage() {
        // given: 用户创建新会话，带首条消息
        Long userId = 1L;
        CreateSessionRequest request = new CreateSessionRequest();
        request.setShopId(100L);
        request.setSubject("订单咨询");
        request.setFirstMessage("你好，我想咨询一下订单");

        when(sessionMapper.selectOne(any())).thenReturn(null);
        when(sessionMapper.insert(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(10L);
            return 1;
        });
        when(messageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(100L);
            return 1;
        });

        // when
        SessionResponse response = sessionService.createSession(userId, request);

        // then
        assertNotNull(response);
        assertEquals(10L, response.getSessionId());
        assertEquals(userId, response.getUserId());
        assertEquals(100L, response.getShopId());
        assertEquals(SessionStatus.IN_PROGRESS.getCode(), response.getStatus());

        verify(sessionMapper).insert(any(ChatSession.class));
        verify(messageMapper).insert(any(ChatMessage.class));
        // 首条消息被插入，会话最后消息被更新
        verify(sessionMapper).updateById(any(ChatSession.class));
    }

    @Test
    void createSessionReusesExistingInProgressSession() {
        // given: 同一用户+同一商家已有进行中会话
        Long userId = 1L;
        CreateSessionRequest request = new CreateSessionRequest();
        request.setShopId(100L);

        ChatSession existingSession = new ChatSession();
        existingSession.setId(10L);
        existingSession.setUserId(userId);
        existingSession.setShopId(100L);
        existingSession.setStatus(SessionStatus.IN_PROGRESS.getCode());
        existingSession.setLastMessage("之前的消息");
        existingSession.setPriority(0);

        when(sessionMapper.selectOne(any())).thenReturn(existingSession);
        when(messageMapper.selectCount(any())).thenReturn(0L);

        // when
        SessionResponse response = sessionService.createSession(userId, request);

        // then: 复用已有会话
        assertNotNull(response);
        assertEquals(10L, response.getSessionId());
        assertEquals(0, response.getUnreadCount());

        // 不应插入新会话或新消息
        verify(sessionMapper, never()).insert(any(ChatSession.class));
        verify(messageMapper, never()).insert(any(ChatMessage.class));
    }

    @Test
    void closeSessionByUserWorks() {
        // given: 用户关闭自己的会话
        Long userId = 1L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setCsUserId(2L);
        session.setStatus(SessionStatus.IN_PROGRESS.getCode());

        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        // when
        sessionService.closeSession(userId, sessionId);

        // then: 会话状态被更新为已结束
        verify(sessionMapper).updateById(any(ChatSession.class));
    }

    @Test
    void closeSessionRejectsNonParticipant() {
        // given: 非会话参与者尝试关闭
        Long hackerUserId = 999L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(1L); // 真正的用户
        session.setCsUserId(2L); // 客服

        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        // when & then: 抛出禁止异常
        assertThrows(BusinessException.class,
                () -> sessionService.closeSession(hackerUserId, sessionId));

        verify(sessionMapper, never()).updateById(any(ChatSession.class));
    }

    @Test
    void assignSessionAllocatesToCustomerService() {
        // given: 客服分配会话
        Long csUserId = 2L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(1L);
        session.setStatus(SessionStatus.IN_PROGRESS.getCode());
        session.setCsUserId(null); // 未分配
        session.setShopId(100L);

        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(messageMapper.selectCount(any())).thenReturn(0L);

        // when
        SessionResponse response = sessionService.assignSession(csUserId, sessionId);

        // then: 客服ID被分配
        assertNotNull(response);
        verify(sessionMapper).updateById(any(ChatSession.class));
    }

    @Test
    void assignSessionRejectsAlreadyAllocatedToOtherCs() {
        // given: 会话已分配给其他客服
        Long csUserId = 2L;
        Long otherCsUserId = 3L;
        Long sessionId = 10L;

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(1L);
        session.setStatus(SessionStatus.IN_PROGRESS.getCode());
        session.setCsUserId(otherCsUserId); // 已分配给其他客服

        when(sessionMapper.selectById(sessionId)).thenReturn(session);

        // when & then: 分配冲突
        assertThrows(BusinessException.class,
                () -> sessionService.assignSession(csUserId, sessionId));

        verify(sessionMapper, never()).updateById(any(ChatSession.class));
    }

    @Test
    void assignSessionRejectsAnotherShopsSessionWithoutRevealingIt() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        session.setShopId(200L);
        session.setStatus(SessionStatus.IN_PROGRESS.getCode());
        when(sessionMapper.selectById(10L)).thenReturn(session);

        assertThrows(BusinessException.class, () -> sessionService.assignSession(2L, 10L));
        verify(sessionMapper, never()).updateById(any(ChatSession.class));
    }

    @Test
    void unreadCountRejectsNonParticipant() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        session.setCsUserId(2L);
        when(sessionMapper.selectById(10L)).thenReturn(session);

        assertThrows(BusinessException.class, () -> sessionService.getUnreadCount(10L, 999L));
        verify(messageMapper, never()).selectCount(any());
    }

    @Test
    void listUserSessionsReturnsUserSessionsWithUnreadCount() {
        // given: 用户有 2 个会话
        Long userId = 1L;
        Long csUserId = 2L;

        ChatSession s1 = new ChatSession();
        s1.setId(10L);
        s1.setUserId(userId);
        s1.setCsUserId(csUserId);
        s1.setShopId(100L);
        s1.setStatus(SessionStatus.IN_PROGRESS.getCode());
        s1.setLastMessage("你好");
        s1.setPriority(0);

        ChatSession s2 = new ChatSession();
        s2.setId(11L);
        s2.setUserId(userId);
        s2.setCsUserId(csUserId);
        s2.setShopId(null);
        s2.setStatus(SessionStatus.IN_PROGRESS.getCode());
        s2.setLastMessage("客服回复");
        s2.setPriority(0);

        when(sessionMapper.selectList(any())).thenReturn(List.of(s1, s2));
        // getOtherPartyUserId 内部需要 selectById
        when(sessionMapper.selectById(10L)).thenReturn(s1);
        when(sessionMapper.selectById(11L)).thenReturn(s2);
        when(messageMapper.selectCount(any())).thenReturn(2L, 0L);

        // when
        List<SessionResponse> result = sessionService.listUserSessions(userId);

        // then
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getUnreadCount());
        assertEquals(0, result.get(1).getUnreadCount());
    }

    private static Shop openShop(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setStatus("OPEN");
        return shop;
    }
}
