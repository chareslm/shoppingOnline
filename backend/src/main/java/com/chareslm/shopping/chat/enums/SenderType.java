package com.chareslm.shopping.chat.enums;

import lombok.Getter;

/**
 * 消息发送方类型。
 */
@Getter
public enum SenderType {

    USER(1, "普通用户"),
    CUSTOMER_SERVICE(2, "客服"),
    SYSTEM(3, "系统");

    private final int code;
    private final String desc;

    SenderType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SenderType fromCode(Integer code) {
        if (code == null) return null;
        for (SenderType t : values()) {
            if (t.code == code) return t;
        }
        return null;
    }
}
