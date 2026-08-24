package com.chareslm.shopping.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("shop_staff")
public class ShopStaff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private Long userId;
    private String displayName;
    private String status;
    private String auditRemark;
    private String emailDeliveryStatus;
    @TableField(exist = false)
    private String shopName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
