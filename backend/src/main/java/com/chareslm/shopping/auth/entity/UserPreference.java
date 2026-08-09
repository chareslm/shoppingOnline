package com.chareslm.shopping.auth.entity;

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
}
