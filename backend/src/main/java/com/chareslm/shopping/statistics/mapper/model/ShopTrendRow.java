package com.chareslm.shopping.statistics.mapper.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ShopTrendRow {
    private LocalDate metricDate;
    private Long paidOrderCount;
    private Long paidBuyerCount;
    private BigDecimal grossPaidAmount;
    private BigDecimal successfulRefundAmount;
    private Long soldQuantity;
}
