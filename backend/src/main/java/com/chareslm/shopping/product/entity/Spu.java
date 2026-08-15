package com.chareslm.shopping.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品 SPU（标准化产品单元）。
 */
@Getter
@Setter
@TableName("spu")
public class Spu extends BaseEntity {

    /** 店铺 ID（引用成员2 shop 表） */
    private Long shopId;

    /** 类目 ID */
    private Long categoryId;

    /** 品牌 */
    private String brand;

    /** 商品名称 */
    private String name;

    /** 副标题/卖点 */
    private String subtitle;

    /** 主图 URL */
    private String mainImage;

    /** 轮播图 URL 数组（JSON 字符串） */
    private String images;

    /** 图文详情 */
    private String detail;

    /** 最低 SKU 价格（冗余） */
    private BigDecimal priceMin;

    /** 最高 SKU 价格（冗余） */
    private BigDecimal priceMax;

    /** 累计销量 */
    private Integer sales;

    /** 平均评分（评价模块回写） */
    private BigDecimal rating;

    /** 状态机：DRAFT/PENDING_AUDIT/AUDIT_APPROVED/AUDIT_REJECTED/ON_SALE/OFF_SALE */
    private String status;

    /** 审核备注/驳回原因 */
    private String auditRemark;
}
