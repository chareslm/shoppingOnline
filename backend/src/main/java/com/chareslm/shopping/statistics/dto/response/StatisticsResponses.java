package com.chareslm.shopping.statistics.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class StatisticsResponses {
    private StatisticsResponses() {
    }

    public record Range(LocalDateTime startAt, LocalDateTime endAt) {
    }

    public record PlatformOverview(String metricVersion, String timezone, OffsetDateTime generatedAt,
                                   OffsetDateTime dataAsOf, Range range, PlatformMetrics metrics) {
    }

    public record PlatformMetrics(long newUsers, long activeUsersSnapshot, long paidOrderCount,
                                  long paidBuyerCount, String grossPaidAmount,
                                  String successfulRefundAmount, String netCashflowActivity,
                                  long onSaleProductSnapshot, long searchCount,
                                  long displayedReviewCount) {
    }

    public record PlatformTrends(String metricVersion, String timezone, OffsetDateTime generatedAt,
                                 OffsetDateTime dataAsOf, Range range,
                                 List<PlatformTrendPoint> points) {
    }

    public record PlatformTrendPoint(LocalDate date, long newUsers, long paidOrderCount,
                                     long paidBuyerCount, String grossPaidAmount,
                                     String successfulRefundAmount, String netCashflowActivity,
                                     long searchCount) {
    }

    public record ShopOverview(String metricVersion, String timezone, OffsetDateTime generatedAt,
                               OffsetDateTime dataAsOf, Range range, Long shopId, String shopName,
                               ShopMetrics metrics) {
    }

    public record ShopMetrics(long paidOrderCount, long paidBuyerCount, String grossPaidAmount,
                              String successfulRefundAmount, String netCashflowActivity,
                              String averageOrderValue, long soldQuantity,
                              long onSaleProductSnapshot, long displayedReviewCount,
                              String averageRating) {
    }

    public record ShopTrends(String metricVersion, String timezone, OffsetDateTime generatedAt,
                             OffsetDateTime dataAsOf, Range range, Long shopId, String shopName,
                             List<ShopTrendPoint> points) {
    }

    public record ShopTrendPoint(LocalDate date, long paidOrderCount, long paidBuyerCount,
                                 String grossPaidAmount, String successfulRefundAmount,
                                 String netCashflowActivity, long soldQuantity) {
    }

    public record UserOverview(String metricVersion, String timezone, OffsetDateTime generatedAt,
                               OffsetDateTime dataAsOf, Range range, UserMetrics metrics) {
    }

    public record UserMetrics(long paidOrderCount, String grossPaidAmount,
                              String successfulRefundAmount, long displayedReviewCount) {
    }
}
