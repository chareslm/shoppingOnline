package com.chareslm.shopping.message.enums;

import lombok.Getter;

/**
 * 通知分类。
 */
@Getter
public enum NotificationCategory {

    SYSTEM(1, "系统通知"),
    ORDER(2, "订单通知"),
    MARKETING(3, "营销通知"),
    SERVICE(4, "客服消息");

    private final int code;
    private final String desc;

    NotificationCategory(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NotificationCategory fromCode(Integer code) {
        if (code == null) return null;
        for (NotificationCategory c : values()) {
            if (c.code == code) return c;
        }
        return null;
    }
}
