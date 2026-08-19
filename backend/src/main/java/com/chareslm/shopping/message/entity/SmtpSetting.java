package com.chareslm.shopping.message.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("system_smtp_setting")
public class SmtpSetting {
    @TableId
    private Integer id;
    private Boolean enabled;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromAddress;
    private Boolean smtpAuth;
    private Boolean starttlsEnabled;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
