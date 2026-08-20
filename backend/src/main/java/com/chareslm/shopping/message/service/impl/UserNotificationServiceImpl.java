package com.chareslm.shopping.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chareslm.shopping.chat.controller.ChatWebSocketHandler;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.message.converter.NotificationConverter;
import com.chareslm.shopping.message.dto.response.NotificationResponse;
import com.chareslm.shopping.message.dto.response.PreferenceResponse;
import com.chareslm.shopping.message.entity.MessageTemplate;
import com.chareslm.shopping.message.entity.NotificationPreference;
import com.chareslm.shopping.message.entity.UserNotification;
import com.chareslm.shopping.message.enums.NotificationCategory;
import com.chareslm.shopping.message.enums.PushStatus;
import com.chareslm.shopping.message.enums.TemplateStatus;
import com.chareslm.shopping.message.event.NotificationSentEvent;
import com.chareslm.shopping.message.mapper.MessageTemplateMapper;
import com.chareslm.shopping.message.mapper.NotificationPreferenceMapper;
import com.chareslm.shopping.message.mapper.UserNotificationMapper;
import com.chareslm.shopping.message.service.UserNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户通知服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationMapper notificationMapper;
    private final MessageTemplateMapper templateMapper;
    private final NotificationPreferenceMapper preferenceMapper;
    private final ChatWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void sendChatNotification(Long userId, Long sessionId, String content) {
        sendNotification(userId, "CS_MESSAGE",
                Map.of("content", content),
                "CHAT_SESSION", String.valueOf(sessionId));
    }

    @Override
    @Transactional
    public void sendNotification(Long userId, String templateCode, Map<String, String> variables,
                                  String bizType, String bizId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "sendNotification");
        MDC.put("templateCode", templateCode);

        MessageTemplate template = templateMapper.selectOne(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, templateCode)
                .eq(MessageTemplate::getStatus, TemplateStatus.ENABLED.getCode())
                .last("LIMIT 1"));
        if (template == null) {
            log.warn("Template not found or disabled: templateCode={}", templateCode);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        String renderedContent = renderTemplate(template.getContent(), variables);
        String renderedTitle = renderTemplate(template.getTitle(), variables);

        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setTemplateId(template.getId());
        notification.setTemplateCode(templateCode);
        notification.setTitle(renderedTitle);
        notification.setContent(renderedContent);
        notification.setCategory(template.getCategory());
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setIsRead(0);
        notification.setPushStatus(template.getPushEnabled() != null
                && template.getPushEnabled() == 1 ? PushStatus.SUCCESS.getCode() : PushStatus.NOT_PUSHED.getCode());
        notification.setPushTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        // 预留：Redis 缓存未读通知数
        // redisTemplate.opsForValue().increment("message:unread:" + userId, 1);

        // 发布领域事件
        NotificationSentEvent event = new NotificationSentEvent(
                notification.getId(), userId, templateCode,
                template.getCategory(), renderedTitle, LocalDateTime.now());
        eventPublisher.publishEvent(event);

        // WebSocket 实时推送给用户
        NotificationResponse response = NotificationConverter.toNotificationResponse(notification);
        webSocketHandler.pushMessage(userId, response);

        log.info("Notification sent: notificationId={}, userId={}, templateCode={}",
                notification.getId(), userId, templateCode);
        MDC.clear();
    }

    @Override
    public List<NotificationResponse> listMyNotifications(Long userId, Integer category, int page, int pageSize) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "listMyNotifications");

        Page<UserNotification> result = notificationMapper.selectPage(
                Page.of(Math.max(page - 1, 0), pageSize),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(category != null, UserNotification::getCategory, category)
                        .orderByDesc(UserNotification::getCreatedAt));

        List<NotificationResponse> responses = result.getRecords().stream()
                .map(NotificationConverter::toNotificationResponse).toList();
        log.info("Listed {} notifications for userId={}", responses.size(), userId);
        MDC.clear();
        return responses;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "markAsRead");
        MDC.put("notificationId", String.valueOf(notificationId));

        UserNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);

        log.info("Notification marked as read: notificationId={}", notificationId);
        MDC.clear();
    }

    @Override
    @Transactional
    public void markBatchAsRead(Long userId, List<Long> notificationIds) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "markBatchAsRead");

        int updated = notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .in(UserNotification::getId, notificationIds)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now()));

        // 预留：Redis 更新未读数
        // redisTemplate.opsForValue().decrement("message:unread:" + userId, updated);

        log.info("Batch marked {} notifications as read for userId={}", updated, userId);
        MDC.clear();
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "markAllAsRead");

        int updated = notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now()));

        // 预留：Redis 清零未读数
        // redisTemplate.delete("message:unread:" + userId);

        log.info("Marked all {} notifications as read for userId={}", updated, userId);
        MDC.clear();
    }

    @Override
    public int getUnreadCount(Long userId) {
        return Math.toIntExact(notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)));
    }

    @Override
    public PreferenceResponse getOrCreatePreference(Long userId) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "getOrCreatePreference");

        NotificationPreference pref = preferenceMapper.selectOne(new LambdaQueryWrapper<NotificationPreference>()
                .eq(NotificationPreference::getUserId, userId)
                .last("LIMIT 1"));
        if (pref == null) {
            pref = new NotificationPreference();
            pref.setUserId(userId);
            pref.setSystemEnabled(1);
            pref.setOrderEnabled(1);
            pref.setMarketingEnabled(0);
            pref.setServiceEnabled(1);
            preferenceMapper.insert(pref);
            log.info("Created default preference for userId={}", userId);
        }
        MDC.clear();
        return NotificationConverter.toPreferenceResponse(pref);
    }

    @Override
    @Transactional
    public PreferenceResponse updatePreference(Long userId, NotificationPreference preference) {
        MDC.put("userId", String.valueOf(userId));
        MDC.put("module", "MESSAGE");
        MDC.put("action", "updatePreference");

        NotificationPreference existing = preferenceMapper.selectOne(new LambdaQueryWrapper<NotificationPreference>()
                .eq(NotificationPreference::getUserId, userId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_PREF_NOT_FOUND);
        }
        if (preference.getSystemEnabled() != null) existing.setSystemEnabled(preference.getSystemEnabled());
        if (preference.getOrderEnabled() != null) existing.setOrderEnabled(preference.getOrderEnabled());
        if (preference.getMarketingEnabled() != null) existing.setMarketingEnabled(preference.getMarketingEnabled());
        if (preference.getServiceEnabled() != null) existing.setServiceEnabled(preference.getServiceEnabled());
        preferenceMapper.updateById(existing);

        log.info("Preference updated for userId={}", userId);
        MDC.clear();
        return NotificationConverter.toPreferenceResponse(existing);
    }

    /**
     * 渲染模板：将 {key} 替换为 variables 中的值。
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
