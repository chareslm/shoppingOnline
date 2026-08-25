package com.chareslm.shopping.statistics.mapper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlatformOverviewRow {
    private Long newUserCount;
    private Long activeUserCount;
    private Long paidOrderCount;
    private Long paidBuyerCount;
    private BigDecimal grossPaidAmount;
    private BigDecimal successfulRefundAmount;
    private Long onSaleProductCount;
    private Long searchCount;
    private Long displayedReviewCount;
}
