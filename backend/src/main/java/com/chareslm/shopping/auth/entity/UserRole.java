package com.chareslm.shopping.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_role")
public class UserRole {
    private Long userId;
    private Long roleId;
    /** Operator that granted a business role; null for legacy/self-registration relations. */
    private Long grantedBy;
}
