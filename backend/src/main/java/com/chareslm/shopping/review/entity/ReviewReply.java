package com.chareslm.shopping.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.chareslm.shopping.common.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 商家回复（一评价一回复）。
 */
@Getter
@Setter
@TableName("review_reply")
public class ReviewReply extends BaseEntity {

    /** 关联评价 */
    private Long reviewId;

    /** 店铺 ID */
    private Long shopId;

    /** 回复内容 */
    private String content;

    /** 回复人 ID（商家账号） */
    private Long repliedBy;
}
