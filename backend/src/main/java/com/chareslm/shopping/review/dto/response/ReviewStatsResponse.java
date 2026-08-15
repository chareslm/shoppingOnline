package com.chareslm.shopping.review.dto.response;

import java.math.BigDecimal;

/**
 * 商品评分聚合。
 */
public record ReviewStatsResponse(
        BigDecimal averageRating,
        long totalCount,
        long fiveStar,
        long fourStar,
        long threeStar,
        long twoStar,
        long oneStar,
        BigDecimal positiveRate
) {
}
