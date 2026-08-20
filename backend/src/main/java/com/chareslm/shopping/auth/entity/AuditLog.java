package com.chareslm.shopping.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long actorUserId;
    private String module;
    private String actionCode;
    private String targetType;
    private String targetId;
    private Boolean success;
    private String traceId;
    private String requestMethod;
    private String requestPath;
    private String clientIp;
    private String userAgent;
    private String detail;
    private java.time.LocalDateTime createdAt;

    @TableField(exist = false)
    private String actorUsername;
}
