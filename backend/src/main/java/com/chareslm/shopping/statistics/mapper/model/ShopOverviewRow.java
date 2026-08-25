package com.chareslm.shopping.statistics.mapper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ShopOverviewRow {
    private Long paidOrderCount;
    private Long paidBuyerCount;
    private BigDecimal grossPaidAmount;
    private BigDecimal successfulRefundAmount;
    private Long soldQuantity;
    private Long onSaleProductCount;
    private Long displayedReviewCount;
    private BigDecimal averageRating;
}
