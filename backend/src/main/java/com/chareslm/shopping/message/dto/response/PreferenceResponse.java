package com.chareslm.shopping.message.dto.response;

/**
 * 用户通知偏好响应 DTO。
 */
public record PreferenceResponse(
        Long id,
        Long userId,
        Integer systemEnabled,
        Integer orderEnabled,
        Integer marketingEnabled,
        Integer serviceEnabled
) {
}
