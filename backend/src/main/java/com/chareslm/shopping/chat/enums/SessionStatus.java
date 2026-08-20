package com.chareslm.shopping.chat.enums;

import lombok.Getter;

/**
 * 客服会话状态。
 */
@Getter
public enum SessionStatus {

    IN_PROGRESS(0, "进行中"),
    CLOSED(1, "已结束");

    private final int code;
    private final String desc;

    SessionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SessionStatus fromCode(Integer code) {
        if (code == null) return null;
        for (SessionStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
