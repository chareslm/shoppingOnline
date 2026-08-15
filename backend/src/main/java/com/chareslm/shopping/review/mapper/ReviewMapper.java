package com.chareslm.shopping.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.review.entity.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评价数据访问。
 * <p>
 * 评价资格校验需要读取交易模块的 order / order_item 表及商品模块的 sku / spu 表。
 * 模块化单体下这些为共享数据库表，通过单条 JOIN 查询校验资格，避免多次往返。
 */
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 查询订单项上下文（评价资格校验 + 归属判定）。
     */
    @Select("""
            SELECT oi.id AS orderItemId, oi.order_id AS orderId, oi.sku_id AS skuId,
                   o.user_id AS userId, o.status AS orderStatus,
                   s.spu_id AS spuId, p.shop_id AS shopId
            FROM order_item oi
            JOIN `order` o ON o.id = oi.order_id
            JOIN sku s ON s.id = oi.sku_id
            JOIN spu p ON p.id = s.spu_id
            WHERE oi.id = #{orderItemId}
            """)
    OrderItemContext selectOrderItemContext(@Param("orderItemId") Long orderItemId);

    /**
     * 评分聚合：平均分、总数、1~5 星分布（仅统计显示中的评价）。
     */
    @Select("""
            SELECT COALESCE(AVG(rating), 0) AS averageRating,
                   COUNT(*) AS totalCount,
                   COALESCE(SUM(rating = 5), 0) AS fiveStar,
                   COALESCE(SUM(rating = 4), 0) AS fourStar,
                   COALESCE(SUM(rating = 3), 0) AS threeStar,
                   COALESCE(SUM(rating = 2), 0) AS twoStar,
                   COALESCE(SUM(rating = 1), 0) AS oneStar
            FROM review
            WHERE spu_id = #{spuId} AND status = 'DISPLAYED'
            """)
    RatingAggregate selectRatingAggregate(@Param("spuId") Long spuId);

    /**
     * 订单项上下文（跨表只读投影）。
     */
    record OrderItemContext(Long orderItemId, Long orderId, Long skuId,
                            Long userId, Integer orderStatus, Long spuId, Long shopId) {
    }

    /**
     * 评分聚合结果。
     */
    record RatingAggregate(java.math.BigDecimal averageRating, Long totalCount,
                           Long fiveStar, Long fourStar, Long threeStar, Long twoStar, Long oneStar) {
    }
}
