package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.dto.request.CreateSessionRequest;
import com.chareslm.shopping.chat.dto.response.SessionResponse;
import com.chareslm.shopping.chat.service.ChatSessionService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客服会话接口。
 * <p>
 * 权限说明：所有接口需登录，普通用户和客服均可访问各自权限范围内的接口。
 */
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;

    @PostMapping
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.success(sessionService.createSession(CurrentUser.require().userId(), request));
    }

    @GetMapping
    public ApiResponse<List<SessionResponse>> listMySessions() {
        return ApiResponse.success(sessionService.listUserSessions(CurrentUser.require().userId()));
    }

    @GetMapping("/cs")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public ApiResponse<List<SessionResponse>> listCsSessions() {
        return ApiResponse.success(sessionService.listCsSessions(CurrentUser.require().userId()));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.getSession(CurrentUser.require().userId(), sessionId));
    }

    @PutMapping("/{sessionId}/assign")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public ApiResponse<SessionResponse> assignSession(@PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.assignSession(CurrentUser.require().userId(), sessionId));
    }

    @PutMapping("/{sessionId}/close")
    public ApiResponse<Void> closeSession(@PathVariable Long sessionId) {
        sessionService.closeSession(CurrentUser.require().userId(), sessionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{sessionId}/unread")
    public ApiResponse<Integer> getUnreadCount(@PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.getUnreadCount(sessionId, CurrentUser.require().userId()));
    }
}
