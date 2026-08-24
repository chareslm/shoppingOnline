package com.chareslm.shopping.chat.controller;

import com.chareslm.shopping.chat.dto.request.SendMessageRequest;
import com.chareslm.shopping.chat.dto.response.MessageResponse;
import com.chareslm.shopping.chat.service.ChatMessageService;
import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天消息接口。
 * <p>
 * 权限说明：会话参与者（用户或已分配客服）才能操作消息。
 */
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;

    @PostMapping("/{sessionId}")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messageService.sendMessage(CurrentUser.require().userId(), sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<List<MessageResponse>> listMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(messageService.listMessages(
                CurrentUser.require().userId(), sessionId, page, pageSize));
    }

    @GetMapping("/{sessionId}/offline")
    public ApiResponse<List<MessageResponse>> pullOfflineMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long lastMessageId) {
        return ApiResponse.success(messageService.pullOfflineMessages(
                CurrentUser.require().userId(), sessionId, lastMessageId));
    }

    @PutMapping("/{sessionId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long sessionId,
            @RequestBody List<Long> messageIds) {
        messageService.markAsRead(CurrentUser.require().userId(), sessionId, messageIds);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> recallMessage(@PathVariable Long messageId) {
        messageService.recallMessage(CurrentUser.require().userId(), messageId);
        return ApiResponse.success(null);
    }
}
