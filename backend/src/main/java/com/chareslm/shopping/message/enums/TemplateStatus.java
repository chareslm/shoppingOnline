package com.chareslm.shopping.message.enums;

import lombok.Getter;

/**
 * 消息模板状态。
 */
@Getter
public enum TemplateStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String desc;

    TemplateStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TemplateStatus fromCode(Integer code) {
        if (code == null) return null;
        for (TemplateStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
