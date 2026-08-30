package com.chareslm.shopping.chat.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建客服会话请求。
 */
@Getter
@Setter
public class CreateSessionRequest {

    /** 目标店铺ID；服务端会校验店铺存在且正常营业。 */
    @NotNull
    private Long shopId;

    /** 会话主题/关联订单号 */
    @Size(max = 255)
    private String subject;

    /** 首条消息内容 */
    @Size(max = 2000)
    private String firstMessage;
}
