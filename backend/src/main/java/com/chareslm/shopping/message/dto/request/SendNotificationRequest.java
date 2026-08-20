package com.chareslm.shopping.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 发送站内信请求。
 */
public record SendNotificationRequest(
        @NotBlank(message = "模板编码不能为空")
        @Size(max = 64, message = "模板编码长度不超过64")
        String templateCode,

        /** 模板变量替换 */
        Map<String, String> variables,

        /** 关联业务类型(如 ORDER) */
        String bizType,

        /** 关联业务ID */
        String bizId
) {
}
