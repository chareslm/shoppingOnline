package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.dto.request.CreateSessionRequest;
import com.chareslm.shopping.chat.dto.response.SessionResponse;
import com.chareslm.shopping.chat.service.ChatMessageService;
import com.chareslm.shopping.chat.service.ChatSessionService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客服会话接口。
 * <p>
 * 权限说明：所有接口需登录，普通用户和客服均可访问各自权限范围内的接口。
 */
@Tag(name = "客服会话", description = "会话创建、查询、分配、关闭")
@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;

    @Operation(summary = "创建会话", description = "用户发起客服会话，若已有进行中会话则直接返回")
    @PostMapping
    public ApiResponse<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.success(sessionService.createSession(CurrentUser.require().userId(), request));
    }

    @Operation(summary = "查询我的会话列表", description = "当前登录用户作为发起方的会话列表")
    @GetMapping
    public ApiResponse<List<SessionResponse>> listMySessions() {
        return ApiResponse.success(sessionService.listUserSessions(CurrentUser.require().userId()));
    }

    @Operation(summary = "客服工作台会话列表", description = "客服查看已分配+待分配的会话，需要 CUSTOMER_SERVICE 角色")
    @GetMapping("/cs")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public ApiResponse<List<SessionResponse>> listCsSessions() {
        return ApiResponse.success(sessionService.listCsSessions(CurrentUser.require().userId()));
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> getSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.getSession(CurrentUser.require().userId(), sessionId));
    }

    @Operation(summary = "客服领取会话", description = "将未分配会话分配给当前客服，需要 CUSTOMER_SERVICE 角色")
    @PutMapping("/{sessionId}/assign")
    @PreAuthorize("hasRole('CUSTOMER_SERVICE')")
    public ApiResponse<SessionResponse> assignSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.assignSession(CurrentUser.require().userId(), sessionId));
    }

    @Operation(summary = "结束会话")
    @PutMapping("/{sessionId}/close")
    public ApiResponse<Void> closeSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        sessionService.closeSession(CurrentUser.require().userId(), sessionId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取会话未读消息数")
    @GetMapping("/{sessionId}/unread")
    public ApiResponse<Integer> getUnreadCount(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        return ApiResponse.success(sessionService.getUnreadCount(sessionId, CurrentUser.require().userId()));
    }
}
