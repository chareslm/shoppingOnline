package com.chareslm.shopping.message.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.message.dto.request.MarkAsReadRequest;
import com.chareslm.shopping.message.dto.response.NotificationResponse;
import com.chareslm.shopping.message.dto.response.PreferenceResponse;
import com.chareslm.shopping.message.entity.NotificationPreference;
import com.chareslm.shopping.message.service.UserNotificationService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户通知接口（站内信）。
 */
@RestController
@RequestMapping("/api/message/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(
            @RequestParam(required = false) Integer category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(notificationService.listMyNotifications(
                CurrentUser.require().userId(), category, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount() {
        return ApiResponse.success(notificationService.getUnreadCount(CurrentUser.require().userId()));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(CurrentUser.require().userId(), notificationId);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-batch")
    public ApiResponse<Void> markBatchRead(@Valid @RequestBody MarkAsReadRequest request) {
        notificationService.markBatchAsRead(CurrentUser.require().userId(), request.notificationIds());
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllAsRead(CurrentUser.require().userId());
        return ApiResponse.success(null);
    }

    @GetMapping("/preference")
    public ApiResponse<PreferenceResponse> getPreference() {
        return ApiResponse.success(notificationService.getOrCreatePreference(CurrentUser.require().userId()));
    }

    @PutMapping("/preference")
    public ApiResponse<PreferenceResponse> updatePreference(@RequestBody NotificationPreference preference) {
        return ApiResponse.success(notificationService.updatePreference(CurrentUser.require().userId(), preference));
    }
}
