package com.chareslm.shopping.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_preference")
public class UserPreference {
    @TableId
    private Long userId;
    private Boolean marketingEnabled;
    private Boolean orderNotificationEnabled;
    private Boolean systemNotificationEnabled;
    private String extraPreferences;
}
