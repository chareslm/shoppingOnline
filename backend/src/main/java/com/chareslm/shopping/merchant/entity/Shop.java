package com.chareslm.shopping.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 资质审核通过后建立的店铺及其所有者关系。 OPEN 为有效经营，SUSPENDED 为已撤销商家权限。
 */
@Getter
@Setter
@TableName("shop")
public class Shop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private Long applicationId;
    private String name;
    private String status;
}
