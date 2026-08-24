package com.chareslm.shopping.message.dto.response;

/**
 * 消息模板响应 DTO。
 */
public record TemplateResponse(
        Long id,
        String templateCode,
        String title,
        String content,
        Integer category,
        String categoryDesc,
        Integer pushEnabled,
        Integer status
) {
}
