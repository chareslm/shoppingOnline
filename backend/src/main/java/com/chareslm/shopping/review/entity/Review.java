package com.chareslm.shopping.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品评价。
 */
@Getter
@Setter
@TableName("review")
public class Review extends BaseEntity {

    /** 关联订单 */
    private Long orderId;

    /** 关联订单项（评价资格：一订单项一评价） */
    private Long orderItemId;

    /** 商品 SPU */
    private Long spuId;

    /** 商品 SKU */
    private Long skuId;

    /** 评价用户 */
    private Long userId;

    /** 店铺 ID */
    private Long shopId;

    /** 评分 1~5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 晒图 URL 数组（JSON 字符串） */
    private String images;

    /** 1 匿名 / 0 实名 */
    private Integer isAnonymous;

    /** DISPLAYED 显示 / HIDDEN 隐藏 */
    private String status;
}
