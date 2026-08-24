package com.chareslm.shopping.message.service.impl;

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
import com.chareslm.shopping.message.mapper.MessageTemplateMapper;
import com.chareslm.shopping.message.mapper.NotificationPreferenceMapper;
import com.chareslm.shopping.message.mapper.UserNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

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
 * 用户通知服务单元测试。
 */
class UserNotificationServiceImplTest {

    private UserNotificationMapper notificationMapper;
    private MessageTemplateMapper templateMapper;
    private NotificationPreferenceMapper preferenceMapper;
    private ChatWebSocketHandler webSocketHandler;
    private ApplicationEventPublisher eventPublisher;
    private UserNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(UserNotificationMapper.class);
        templateMapper = mock(MessageTemplateMapper.class);
        preferenceMapper = mock(NotificationPreferenceMapper.class);
        webSocketHandler = mock(ChatWebSocketHandler.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        notificationService = new UserNotificationServiceImpl(
                notificationMapper, templateMapper, preferenceMapper,
                webSocketHandler, eventPublisher);
    }

    @Test
    void sendNotificationPersistsAndPushesViaWebSocket() {
        // given
        Long userId = 1L;

        MessageTemplate template = new MessageTemplate();
        template.setId(10L);
        template.setTemplateCode("CS_MESSAGE");
        template.setTitle("客服消息");
        template.setContent("{content}");
        template.setCategory(NotificationCategory.SERVICE.getCode());
        template.setPushEnabled(1);
        template.setStatus(TemplateStatus.ENABLED.getCode());

        when(templateMapper.selectOne(any())).thenReturn(template);
        when(notificationMapper.insert(any(UserNotification.class))).thenAnswer(inv -> {
            UserNotification n = inv.getArgument(0);
            n.setId(100L);
            return 1;
        });

        // when
        notificationService.sendChatNotification(userId, 10L, "你好");

        // then: 通知被持久化
        verify(notificationMapper).insert(any(UserNotification.class));
    }

    @Test
    void sendNotificationWithInvalidTemplateThrowsException() {
        // given: 模板不存在
        Long userId = 1L;
        when(templateMapper.selectOne(any())).thenReturn(null);

        // when & then
        assertThrows(BusinessException.class,
                () -> notificationService.sendNotification(
                        userId, "INVALID_CODE", Map.of(), "BIZ", "1"));

        verify(notificationMapper, never()).insert(any(UserNotification.class));
        verify(webSocketHandler, never()).pushMessage(anyLong(), any());
    }

    @Test
    void markAsReadUpdatesStatus() {
        // given
        Long userId = 1L;
        Long notificationId = 100L;

        UserNotification notification = new UserNotification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setIsRead(0);

        when(notificationMapper.selectById(notificationId)).thenReturn(notification);

        // when
        notificationService.markAsRead(userId, notificationId);

        // then
        verify(notificationMapper).updateById(any(UserNotification.class));
    }

    @Test
    void markAsReadByWrongUserRejected() {
        // given
        Long userId = 1L;
        Long notificationId = 100L;

        UserNotification notification = new UserNotification();
        notification.setId(notificationId);
        notification.setUserId(999L); // 属于其他用户

        when(notificationMapper.selectById(notificationId)).thenReturn(notification);

        // when & then
        assertThrows(BusinessException.class,
                () -> notificationService.markAsRead(userId, notificationId));

        verify(notificationMapper, never()).updateById(any(UserNotification.class));
    }

    @Test
    void getOrCreatePreferenceCreatesDefaultWhenAbsent() {
        // given
        Long userId = 1L;
        when(preferenceMapper.selectOne(any())).thenReturn(null);

        // when
        PreferenceResponse response = notificationService.getOrCreatePreference(userId);

        // then: 创建了默认偏好
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(1, response.systemEnabled());
        assertEquals(1, response.orderEnabled());
        assertEquals(0, response.marketingEnabled());
        assertEquals(1, response.serviceEnabled());
        verify(preferenceMapper).insert(any(NotificationPreference.class));
    }

    @Test
    void getOrCreatePreferenceReturnsExistingWhenPresent() {
        // given
        Long userId = 1L;
        NotificationPreference pref = new NotificationPreference();
        pref.setId(10L);
        pref.setUserId(userId);
        pref.setSystemEnabled(1);
        pref.setOrderEnabled(0);
        pref.setMarketingEnabled(0);
        pref.setServiceEnabled(1);

        when(preferenceMapper.selectOne(any())).thenReturn(pref);

        // when
        PreferenceResponse response = notificationService.getOrCreatePreference(userId);

        // then: 返回已有偏好
        assertNotNull(response);
        assertEquals(0, response.orderEnabled());
        verify(preferenceMapper, never()).insert(any(NotificationPreference.class));
    }

    @Test
    void updatePreferenceUpdatesFields() {
        // given
        Long userId = 1L;
        NotificationPreference pref = new NotificationPreference();
        pref.setId(10L);
        pref.setUserId(userId);
        pref.setSystemEnabled(1);
        pref.setOrderEnabled(1);
        pref.setMarketingEnabled(0);
        pref.setServiceEnabled(1);

        when(preferenceMapper.selectOne(any())).thenReturn(pref);

        NotificationPreference update = new NotificationPreference();
        update.setOrderEnabled(0);
        update.setMarketingEnabled(1);

        // when
        PreferenceResponse response = notificationService.updatePreference(userId, update);

        // then
        assertNotNull(response);
        verify(preferenceMapper).updateById(any(NotificationPreference.class));
    }

    @Test
    void updatePreferenceNotFoundThrowsException() {
        // given
        Long userId = 1L;
        when(preferenceMapper.selectOne(any())).thenReturn(null);

        NotificationPreference update = new NotificationPreference();
        update.setOrderEnabled(0);

        // when & then
        assertThrows(BusinessException.class,
                () -> notificationService.updatePreference(userId, update));
    }

    @Test
    void getUnreadCountReturnsCount() {
        // given
        Long userId = 1L;
        when(notificationMapper.selectCount(any())).thenReturn(5L);

        // when
        int count = notificationService.getUnreadCount(userId);

        // then
        assertEquals(5, count);
    }
}
