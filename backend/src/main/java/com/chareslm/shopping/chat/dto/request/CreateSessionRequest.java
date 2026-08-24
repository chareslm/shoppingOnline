package com.chareslm.shopping.chat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建客服会话请求。
 */
@Getter
@Setter
public class CreateSessionRequest {

    /** 商家ID（可为空=平台客服） */
    private Long shopId;

    /** 会话主题/关联订单号 */
    @Size(max = 255)
    private String subject;

    /** 首条消息内容 */
    @Size(max = 2000)
    private String firstMessage;
}
