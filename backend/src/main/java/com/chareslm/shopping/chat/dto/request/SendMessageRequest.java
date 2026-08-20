package com.chareslm.shopping.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 发送聊天消息请求。
 */
@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不能超过5000字")
    private String content;

    /** 消息类型: 1文本/2图片/3商品卡片/4系统通知 */
    private Integer msgType = 1;

    /** 扩展数据(如商品卡片JSON) */
    private String extraData;
}
