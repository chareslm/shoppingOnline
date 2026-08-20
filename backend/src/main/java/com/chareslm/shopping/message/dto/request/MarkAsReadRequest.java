package com.chareslm.shopping.message.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量标记已读请求。
 */
public record MarkAsReadRequest(
        @NotEmpty(message = "通知ID列表不能为空")
        List<Long> notificationIds
) {
}
