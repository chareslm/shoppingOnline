package com.chareslm.shopping.message.service;

import java.util.List;
import java.util.Map;

import com.chareslm.shopping.message.dto.response.NotificationResponse;
import com.chareslm.shopping.message.dto.response.PreferenceResponse;

/**
 * 用户通知服务。
 */
public interface UserNotificationService {

    /**
     * 发送客服聊天通知。
     */
    void sendChatNotification(Long userId, Long sessionId, String content);

    /**
     * 基于模板发送通知。
     *
     * @param userId        接收用户ID
     * @param templateCode  模板编码
     * @param variables     模板变量
     * @param bizType       关联业务类型(可选)
     * @param bizId         关联业务ID(可选)
     */
    void sendNotification(Long userId, String templateCode, Map<String, String> variables,
                           String bizType, String bizId);

    /**
     * 查询我的通知列表（分页, 按创建时间倒序）。
     */
    List<NotificationResponse> listMyNotifications(Long userId, Integer category, int page, int pageSize);

    /**
     * 标记单条通知为已读。
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 批量标记通知为已读。
     */
    void markBatchAsRead(Long userId, List<Long> notificationIds);

    /**
     * 标记全部通知为已读。
     */
    void markAllAsRead(Long userId);

    /**
     * 获取未读通知数量。
     */
    int getUnreadCount(Long userId);

    /**
     * 获取或创建用户通知偏好。
     */
    PreferenceResponse getOrCreatePreference(Long userId);

    /**
     * 更新用户通知偏好。
     */
    PreferenceResponse updatePreference(Long userId, com.chareslm.shopping.message.entity.NotificationPreference preference);
}
