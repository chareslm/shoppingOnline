package com.chareslm.shopping.message.enums;

import lombok.Getter;

/**
 * 推送状态。
 */
@Getter
public enum PushStatus {

    NOT_PUSHED(0, "未推送"),
    SUCCESS(1, "推送成功"),
    FAILED(2, "推送失败");

    private final int code;
    private final String desc;

    PushStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PushStatus fromCode(Integer code) {
        if (code == null) return null;
        for (PushStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
