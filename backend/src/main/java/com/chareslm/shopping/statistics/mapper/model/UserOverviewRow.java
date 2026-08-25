package com.chareslm.shopping.statistics.mapper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserOverviewRow {
    private Long paidOrderCount;
    private BigDecimal grossPaidAmount;
    private BigDecimal successfulRefundAmount;
    private Long displayedReviewCount;
}
