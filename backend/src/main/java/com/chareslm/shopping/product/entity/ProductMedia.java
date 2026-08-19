package com.chareslm.shopping.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_media")
public class ProductMedia extends BaseEntity {
    private Long shopId;
    private String storageKey;
    private String contentType;
    private String originalName;
    private Long fileSize;
}
