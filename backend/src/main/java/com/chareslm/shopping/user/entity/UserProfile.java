package com.chareslm.shopping.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("user_profile")
public class UserProfile {
    @TableId
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String realName;
    private String gender;
    private LocalDate birthday;
    private String bio;
}
