package com.chareslm.shopping.chat.enums;

import lombok.Getter;

/**
 * 聊天消息类型。
 */
@Getter
public enum MessageType {

    TEXT(1, "文本"),
    IMAGE(2, "图片"),
    PRODUCT_CARD(3, "商品卡片"),
    SYSTEM(4, "系统通知");

    private final int code;
    private final String desc;

    MessageType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MessageType fromCode(Integer code) {
        if (code == null) return null;
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }
}
