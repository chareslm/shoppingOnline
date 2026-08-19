package com.chareslm.shopping.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_device")
public class UserDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String appVersion;
    private String lastIp;
    private LocalDateTime lastActiveAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
