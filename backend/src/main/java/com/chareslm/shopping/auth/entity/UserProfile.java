package com.chareslm.shopping.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_profile")
public class UserProfile {
    @TableId
    private Long userId;
    private String nickname;
}
