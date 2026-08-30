package com.chareslm.shopping.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chareslm.shopping.chat.converter.ChatSessionConverter;
import com.chareslm.shopping.chat.dto.request.CreateSessionRequest;
import com.chareslm.shopping.chat.dto.response.SessionResponse;
import com.chareslm.shopping.chat.entity.ChatMessage;
import com.chareslm.shopping.chat.entity.ChatSession;
import com.chareslm.shopping.chat.enums.MessageType;
import com.chareslm.shopping.chat.enums.SenderType;
import com.chareslm.shopping.chat.enums.SessionStatus;
import com.chareslm.shopping.chat.event.SessionCreatedEvent;
import com.chareslm.shopping.chat.mapper.ChatMessageMapper;
import com.chareslm.shopping.chat.mapper.ChatSessionMapper;
import com.chareslm.shopping.chat.service.ChatSessionService;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 客服会话服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final MerchantShopQueryService merchantShopQueryService;

    @Override
    @Transactional
    public SessionResponse createSession(Long userId, CreateSessionRequest request) {
        Shop targetShop = merchantShopQueryService.requireOpenShopById(request.getShopId());
        // 结构化日志
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "createSession");
        log.info("Creating chat session for userId={}, shopId={}", userId, targetShop.getId());

        // 查询是否已有进行中的会话（同一用户+同一商家）
        ChatSession existing = sessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getShopId, targetShop.getId())
                .eq(ChatSession::getStatus, SessionStatus.IN_PROGRESS.getCode())
                .last("LIMIT 1"));
        if (existing != null) {
            log.info("Reusing existing sessionId={}", existing.getId());
            int unread = computeUnreadCount(existing.getId(), userId);
            return ChatSessionConverter.toResponse(existing, unread);
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setShopId(targetShop.getId());
        session.setSubject(request.getSubject());
        session.setStatus(SessionStatus.IN_PROGRESS.getCode());
        session.setPriority(0);
        sessionMapper.insert(session);

        // 如果有首条消息, 直接创建
        if (request.getFirstMessage() != null && !request.getFirstMessage().isBlank()) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(session.getId());
            msg.setSenderId(userId);
            msg.setSenderType(SenderType.USER.getCode());
            msg.setContent(request.getFirstMessage());
            msg.setMsgType(MessageType.TEXT.getCode());
            msg.setIsRead(0);
            msg.setStatus(1);
            messageMapper.insert(msg);

            session.setLastMessage(msg.getContent());
            session.setLastMessageTime(msg.getCreatedAt());
            sessionMapper.updateById(session);
        }

        // 预留：Redis 缓存会话在线状态
        // redisTemplate.opsForValue().set("chat:session:" + session.getId(), ...);

        // 发布事件
        SessionCreatedEvent event = new SessionCreatedEvent(
                session.getId(), session.getUserId(), session.getShopId(),
                session.getCsUserId(), session.getCreatedAt());
        log.info("Session created successfully, sessionId={}", session.getId());

        MDC.clear();
        return ChatSessionConverter.toResponse(session, 0);
    }

    @Override
    public List<SessionResponse> listUserSessions(Long userId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "listUserSessions");

        List<ChatSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getLastMessageTime));

        List<SessionResponse> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            result.add(ChatSessionConverter.toResponse(s, computeUnreadCount(s.getId(), userId)));
        }
        log.info("Listed {} sessions for userId={}", result.size(), userId);
        MDC.clear();
        return result;
    }

    @Override
    public List<SessionResponse> listCsSessions(Long csUserId) {
        MDC.put("userId", String.valueOf(csUserId));
        MDC.put("module", "CHAT");
        MDC.put("action", "listCsSessions");

        Shop staffShop = merchantShopQueryService.requireOpenStaffShop(csUserId);

        // 客服只能看到本店内已分配给自己的会话和待分配会话。
        List<ChatSession> assigned = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getCsUserId, csUserId)
                .eq(ChatSession::getShopId, staffShop.getId())
                .eq(ChatSession::getStatus, SessionStatus.IN_PROGRESS.getCode())
                .orderByDesc(ChatSession::getLastMessageTime));
        List<ChatSession> unassigned = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .isNull(ChatSession::getCsUserId)
                .eq(ChatSession::getShopId, staffShop.getId())
                .eq(ChatSession::getStatus, SessionStatus.IN_PROGRESS.getCode())
                .orderByAsc(ChatSession::getPriority)
                .orderByAsc(ChatSession::getCreatedAt)
                .last("LIMIT 20"));

        List<ChatSession> merged = new ArrayList<>();
        merged.addAll(assigned);
        merged.addAll(unassigned);

        List<SessionResponse> result = new ArrayList<>();
        for (ChatSession s : merged) {
            result.add(ChatSessionConverter.toResponse(s, computeUnreadCount(s.getId(), csUserId)));
        }
        log.info("CS userId={} listed {} sessions (assigned={}, unassigned={})",
                csUserId, result.size(), assigned.size(), unassigned.size());
        MDC.clear();
        return result;
    }

    @Override
    public SessionResponse getSession(Long userId, Long sessionId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "getSession");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = requireAccessibleSession(userId, sessionId);
        int unread = computeUnreadCount(sessionId, userId);
        SessionResponse resp = ChatSessionConverter.toResponse(session, unread);
        MDC.clear();
        return resp;
    }

    @Override
    @Transactional
    public SessionResponse assignSession(Long csUserId, Long sessionId) {
        MDC.put("userId", String.valueOf(csUserId));
        MDC.put("module", "CHAT");
        MDC.put("action", "assignSession");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || SessionStatus.IN_PROGRESS.getCode() != session.getStatus()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Shop staffShop = merchantShopQueryService.requireOpenStaffShop(csUserId);
        if (!staffShop.getId().equals(session.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (session.getCsUserId() != null && !session.getCsUserId().equals(csUserId)) {
            throw new BusinessException(ErrorCode.SESSION_ALLOCATION_CONFLICT);
        }
        session.setCsUserId(csUserId);
        sessionMapper.updateById(session);

        log.info("Session sessionId={} assigned to csUserId={}", sessionId, csUserId);
        MDC.clear();
        return ChatSessionConverter.toResponse(session, computeUnreadCount(sessionId, csUserId));
    }

    @Override
    @Transactional
    public void closeSession(Long userId, Long sessionId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "CHAT");
        MDC.put("action", "closeSession");
        MDC.put("sessionId", String.valueOf(sessionId));

        ChatSession session = requireAccessibleSession(userId, sessionId);
        if (!session.getUserId().equals(userId) && !userId.equals(session.getCsUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        session.setStatus(SessionStatus.CLOSED.getCode());
        sessionMapper.updateById(session);

        // 预留：Redis 清除会话在线状态
        // redisTemplate.delete("chat:session:" + sessionId);

        log.info("Session sessionId={} closed by userId={}", sessionId, userId);
        MDC.clear();
    }

    @Override
    public int getUnreadCount(Long sessionId, Long userId) {
        requireAccessibleSession(userId, sessionId);
        return computeUnreadCount(sessionId, userId);
    }

    /**
     * 计算会话中指定用户的未读消息数（对方发来的未读消息）。
     */
    private int computeUnreadCount(Long sessionId, Long userId) {
        Long otherUserId = getOtherPartyUserId(sessionId, userId);
        if (otherUserId == null) {
            return 0;
        }
        return Math.toIntExact(messageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getSenderId, otherUserId)
                .eq(ChatMessage::getIsRead, 0)));
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

    private Long getOtherPartyUserId(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) return null;
        if (session.getUserId().equals(userId)) {
            return session.getCsUserId();
        }
        return session.getUserId();
    }
}
