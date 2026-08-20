package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.dto.request.SendMessageRequest;
import com.chareslm.shopping.chat.dto.response.MessageResponse;
import com.chareslm.shopping.chat.service.ChatMessageService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天消息接口。
 * <p>
 * 权限说明：会话参与者（用户或已分配客服）才能操作消息。
 */
@Tag(name = "聊天消息", description = "消息发送、查询、离线拉取、已读标记、撤回")
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;

    @Operation(summary = "发送消息", description = "向指定会话发送消息，支持文本/图片/商品卡片等类型")
    @PostMapping("/{sessionId}")
    public ApiResponse<MessageResponse> sendMessage(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messageService.sendMessage(CurrentUser.require().userId(), sessionId, request));
    }

    @Operation(summary = "查询会话消息列表", description = "分页获取会话历史消息，按时间正序")
    @GetMapping("/{sessionId}")
    public ApiResponse<List<MessageResponse>> listMessages(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(messageService.listMessages(
                CurrentUser.require().userId(), sessionId, page, pageSize));
    }

    @Operation(summary = "拉取离线消息", description = "从指定消息ID之后拉取新消息，用于断线重连后补拉")
    @GetMapping("/{sessionId}/offline")
    public ApiResponse<List<MessageResponse>> pullOfflineMessages(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @Parameter(description = "最后一条已知消息ID")
            @RequestParam(required = false) Long lastMessageId) {
        return ApiResponse.success(messageService.pullOfflineMessages(
                CurrentUser.require().userId(), sessionId, lastMessageId));
    }

    @Operation(summary = "标记消息为已读")
    @PutMapping("/{sessionId}/read")
    public ApiResponse<Void> markAsRead(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestBody List<Long> messageIds) {
        messageService.markAsRead(CurrentUser.require().userId(), sessionId, messageIds);
        return ApiResponse.success(null);
    }

    @Operation(summary = "撤回消息", description = "发送方本人在5分钟内可撤回消息")
    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> recallMessage(
            @Parameter(description = "消息ID") @PathVariable Long messageId) {
        messageService.recallMessage(CurrentUser.require().userId(), messageId);
        return ApiResponse.success(null);
    }
}
