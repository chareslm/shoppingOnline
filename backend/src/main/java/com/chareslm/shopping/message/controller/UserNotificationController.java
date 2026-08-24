package com.chareslm.shopping.message.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.message.dto.request.MarkAsReadRequest;
import com.chareslm.shopping.message.dto.response.NotificationResponse;
import com.chareslm.shopping.message.dto.response.PreferenceResponse;
import com.chareslm.shopping.message.entity.NotificationPreference;
import com.chareslm.shopping.message.service.UserNotificationService;
import com.chareslm.shopping.security.context.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户通知接口（站内信）。
 */
@Tag(name = "用户通知", description = "站内信、通知列表、未读管理")
@RestController
@RequestMapping("/api/message/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService notificationService;

    @Operation(summary = "查询我的通知列表", description = "支持按分类筛选，分页返回")
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(
            @Parameter(description = "通知分类: 1系统/2订单/3营销/4客服")
            @RequestParam(required = false) Integer category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(notificationService.listMyNotifications(
                CurrentUser.require().userId(), category, page, pageSize));
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount() {
        return ApiResponse.success(notificationService.getUnreadCount(CurrentUser.require().userId()));
    }

    @Operation(summary = "标记单条通知为已读")
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(CurrentUser.require().userId(), notificationId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "批量标记通知为已读")
    @PutMapping("/read-batch")
    public ApiResponse<Void> markBatchRead(@Valid @RequestBody MarkAsReadRequest request) {
        notificationService.markBatchAsRead(CurrentUser.require().userId(), request.notificationIds());
        return ApiResponse.success(null);
    }

    @Operation(summary = "标记全部通知为已读")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllAsRead(CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取我的通知偏好设置")
    @GetMapping("/preference")
    public ApiResponse<PreferenceResponse> getPreference() {
        return ApiResponse.success(notificationService.getOrCreatePreference(CurrentUser.require().userId()));
    }

    @Operation(summary = "更新我的通知偏好")
    @PutMapping("/preference")
    public ApiResponse<PreferenceResponse> updatePreference(@RequestBody NotificationPreference preference) {
        return ApiResponse.success(notificationService.updatePreference(CurrentUser.require().userId(), preference));
    }
}
