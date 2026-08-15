package com.chareslm.shopping.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品类目。
 */
@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {

    /** 父类目 ID，0 为根 */
    private Long parentId;

    /** 类目名称 */
    private String name;

    /** 层级：1/2/3 */
    private Integer level;

    /** 排序值，越小越靠前 */
    private Integer sortOrder;

    /** 类目图标 URL */
    private String icon;

    /** 1 启用 / 0 停用 */
    private Integer status;
}
