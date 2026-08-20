package com.chareslm.shopping.message.converter;

import com.chareslm.shopping.message.dto.response.NotificationResponse;
import com.chareslm.shopping.message.dto.response.PreferenceResponse;
import com.chareslm.shopping.message.dto.response.TemplateResponse;
import com.chareslm.shopping.message.entity.MessageTemplate;
import com.chareslm.shopping.message.entity.NotificationPreference;
import com.chareslm.shopping.message.entity.UserNotification;

/**
 * 通知相关实体 ↔ DTO 转换。
 */
public final class NotificationConverter {

    private NotificationConverter() {
    }

    /**
     * UserNotification Entity → NotificationResponse DTO。
     */
    public static NotificationResponse toNotificationResponse(UserNotification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTemplateId(),
                n.getTemplateCode(),
                n.getTitle(),
                n.getContent(),
                n.getCategory(),
                com.chareslm.shopping.message.enums.NotificationCategory.fromCode(n.getCategory()) != null
                        ? com.chareslm.shopping.message.enums.NotificationCategory.fromCode(n.getCategory()).getDesc()
                        : null,
                n.getBizType(),
                n.getBizId(),
                n.getIsRead(),
                n.getReadTime(),
                n.getPushStatus(),
                n.getPushTime(),
                n.getCreatedAt()
        );
    }

    /**
     * NotificationPreference Entity → PreferenceResponse DTO。
     */
    public static PreferenceResponse toPreferenceResponse(NotificationPreference p) {
        return new PreferenceResponse(
                p.getId(),
                p.getUserId(),
                p.getSystemEnabled(),
                p.getOrderEnabled(),
                p.getMarketingEnabled(),
                p.getServiceEnabled()
        );
    }

    /**
     * MessageTemplate Entity → TemplateResponse DTO。
     */
    public static TemplateResponse toTemplateResponse(MessageTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getTemplateCode(),
                t.getTitle(),
                t.getContent(),
                t.getCategory(),
                com.chareslm.shopping.message.enums.NotificationCategory.fromCode(t.getCategory()) != null
                        ? com.chareslm.shopping.message.enums.NotificationCategory.fromCode(t.getCategory()).getDesc()
                        : null,
                t.getPushEnabled(),
                t.getStatus()
        );
    }
}
