package com.chareslm.shopping.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品 SKU（库存保有单位）。
 * <p>
 * 库存权威在 MySQL：available_stock（可售）/ reserved_stock（预占）/ sold_stock（已售）。
 */
@Getter
@Setter
@TableName("sku")
public class Sku extends BaseEntity {

    /** 所属 SPU */
    private Long spuId;

    /** 商家自定义 SKU 编码 */
    private String skuCode;

    /** 规格属性（JSON 字符串），如 {"颜色":"黑色","内存":"256GB"} */
    private String attributes;

    /** SKU 图 URL */
    private String image;

    /** 销售价 */
    private BigDecimal price;

    /** 可售库存 */
    private Integer availableStock;

    /** 预占库存 */
    private Integer reservedStock;

    /** 已售库存 */
    private Integer soldStock;

    /** 1 启用 / 0 停用 */
    private Integer status;
}
