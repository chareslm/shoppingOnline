package com.chareslm.shopping.statistics.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.statistics.dto.response.StatisticsResponses;
import com.chareslm.shopping.statistics.mapper.StatisticsMapper;
import com.chareslm.shopping.statistics.mapper.model.PlatformOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.PlatformTrendRow;
import com.chareslm.shopping.statistics.mapper.model.ShopOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.ShopTrendRow;
import com.chareslm.shopping.statistics.mapper.model.UserOverviewRow;
import com.chareslm.shopping.statistics.service.StatisticsQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsQueryServiceImpl implements StatisticsQueryService {
    static final String METRIC_VERSION = "v1";
    static final String BUSINESS_TIMEZONE = "Asia/Shanghai";
    private static final String DAY = "DAY";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of(BUSINESS_TIMEZONE);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final StatisticsMapper statisticsMapper;
    private final MerchantShopQueryService merchantShopQueryService;

    public StatisticsQueryServiceImpl(StatisticsMapper statisticsMapper,
                                      MerchantShopQueryService merchantShopQueryService) {
        this.statisticsMapper = statisticsMapper;
        this.merchantShopQueryService = merchantShopQueryService;
    }

    @Override
    public StatisticsResponses.PlatformOverview getPlatformOverview(LocalDateTime startAt, LocalDateTime endAt,
                                                                     String timezone, String granularity) {
        QueryRange range = validate(startAt, endAt, timezone, granularity);
        PlatformOverviewRow row = statisticsMapper.selectPlatformOverview(range.startAt(), range.endAt());
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        BigDecimal gross = money(row == null ? null : row.getGrossPaidAmount());
        BigDecimal refund = money(row == null ? null : row.getSuccessfulRefundAmount());
        var metrics = new StatisticsResponses.PlatformMetrics(
                value(row == null ? null : row.getNewUserCount()),
                value(row == null ? null : row.getActiveUserCount()),
                value(row == null ? null : row.getPaidOrderCount()),
                value(row == null ? null : row.getPaidBuyerCount()),
                amount(gross), amount(refund), amount(gross.subtract(refund)),
                value(row == null ? null : row.getOnSaleProductCount()),
                value(row == null ? null : row.getSearchCount()),
                value(row == null ? null : row.getDisplayedReviewCount()));
        return new StatisticsResponses.PlatformOverview(METRIC_VERSION, BUSINESS_TIMEZONE, now, now,
                responseRange(range), metrics);
    }

    @Override
    public StatisticsResponses.PlatformTrends getPlatformTrends(LocalDateTime startAt, LocalDateTime endAt,
                                                                 String timezone, String granularity) {
        QueryRange range = validate(startAt, endAt, timezone, granularity);
        Map<LocalDate, PlatformTrendRow> rows = statisticsMapper.selectPlatformTrends(range.startAt(), range.endAt())
                .stream().collect(Collectors.toMap(PlatformTrendRow::getMetricDate, Function.identity()));
        List<StatisticsResponses.PlatformTrendPoint> points = new ArrayList<>();
        for (LocalDate date : range.dates()) {
            PlatformTrendRow row = rows.get(date);
            BigDecimal gross = money(row == null ? null : row.getGrossPaidAmount());
            BigDecimal refund = money(row == null ? null : row.getSuccessfulRefundAmount());
            points.add(new StatisticsResponses.PlatformTrendPoint(date,
                    value(row == null ? null : row.getNewUserCount()),
                    value(row == null ? null : row.getPaidOrderCount()),
                    value(row == null ? null : row.getPaidBuyerCount()),
                    amount(gross), amount(refund), amount(gross.subtract(refund)),
                    value(row == null ? null : row.getSearchCount())));
        }
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        return new StatisticsResponses.PlatformTrends(METRIC_VERSION, BUSINESS_TIMEZONE, now, now,
                responseRange(range), points);
    }

    @Override
    public StatisticsResponses.ShopOverview getShopOverview(Long userId, LocalDateTime startAt,
                                                             LocalDateTime endAt, String timezone,
                                                             String granularity) {
        QueryRange range = validate(startAt, endAt, timezone, granularity);
        Shop shop = merchantShopQueryService.requireOpenOwnedShop(userId);
        ShopOverviewRow row = statisticsMapper.selectShopOverview(shop.getId(), range.startAt(), range.endAt());
        BigDecimal gross = money(row == null ? null : row.getGrossPaidAmount());
        BigDecimal refund = money(row == null ? null : row.getSuccessfulRefundAmount());
        long paidOrders = value(row == null ? null : row.getPaidOrderCount());
        String averageOrderValue = paidOrders == 0 ? null
                : amount(gross.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP));
        BigDecimal averageRating = row == null ? null : row.getAverageRating();
        var metrics = new StatisticsResponses.ShopMetrics(paidOrders,
                value(row == null ? null : row.getPaidBuyerCount()), amount(gross), amount(refund),
                amount(gross.subtract(refund)), averageOrderValue,
                value(row == null ? null : row.getSoldQuantity()),
                value(row == null ? null : row.getOnSaleProductCount()),
                value(row == null ? null : row.getDisplayedReviewCount()),
                averageRating == null ? null : averageRating.setScale(2, RoundingMode.HALF_UP).toPlainString());
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        return new StatisticsResponses.ShopOverview(METRIC_VERSION, BUSINESS_TIMEZONE, now, now,
                responseRange(range), shop.getId(), shop.getName(), metrics);
    }

    @Override
    public StatisticsResponses.ShopTrends getShopTrends(Long userId, LocalDateTime startAt,
                                                         LocalDateTime endAt, String timezone,
                                                         String granularity) {
        QueryRange range = validate(startAt, endAt, timezone, granularity);
        Shop shop = merchantShopQueryService.requireOpenOwnedShop(userId);
        Map<LocalDate, ShopTrendRow> rows = statisticsMapper
                .selectShopTrends(shop.getId(), range.startAt(), range.endAt()).stream()
                .collect(Collectors.toMap(ShopTrendRow::getMetricDate, Function.identity()));
        List<StatisticsResponses.ShopTrendPoint> points = new ArrayList<>();
        for (LocalDate date : range.dates()) {
            ShopTrendRow row = rows.get(date);
            BigDecimal gross = money(row == null ? null : row.getGrossPaidAmount());
            BigDecimal refund = money(row == null ? null : row.getSuccessfulRefundAmount());
            points.add(new StatisticsResponses.ShopTrendPoint(date,
                    value(row == null ? null : row.getPaidOrderCount()),
                    value(row == null ? null : row.getPaidBuyerCount()),
                    amount(gross), amount(refund), amount(gross.subtract(refund)),
                    value(row == null ? null : row.getSoldQuantity())));
        }
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        return new StatisticsResponses.ShopTrends(METRIC_VERSION, BUSINESS_TIMEZONE, now, now,
                responseRange(range), shop.getId(), shop.getName(), points);
    }

    @Override
    public StatisticsResponses.UserOverview getUserOverview(Long userId, LocalDateTime startAt,
                                                             LocalDateTime endAt, String timezone,
                                                             String granularity) {
        QueryRange range = validate(startAt, endAt, timezone, granularity);
        UserOverviewRow row = statisticsMapper.selectUserOverview(userId, range.startAt(), range.endAt());
        var metrics = new StatisticsResponses.UserMetrics(
                value(row == null ? null : row.getPaidOrderCount()),
                amount(row == null ? null : row.getGrossPaidAmount()),
                amount(row == null ? null : row.getSuccessfulRefundAmount()),
                value(row == null ? null : row.getDisplayedReviewCount()));
        OffsetDateTime now = OffsetDateTime.now(BUSINESS_ZONE);
        return new StatisticsResponses.UserOverview(METRIC_VERSION, BUSINESS_TIMEZONE, now, now,
                responseRange(range), metrics);
    }

    private QueryRange validate(LocalDateTime startAt, LocalDateTime endAt,
                                String timezone, String granularity) {
        String actualTimezone = timezone == null || timezone.isBlank() ? BUSINESS_TIMEZONE : timezone;
        String actualGranularity = granularity == null || granularity.isBlank()
                ? DAY : granularity.toUpperCase(Locale.ROOT);
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)
                || !BUSINESS_TIMEZONE.equals(actualTimezone) || !DAY.equals(actualGranularity)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        LocalDate lastDate = endAt.minusNanos(1).toLocalDate();
        long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), lastDate) + 1;
        if (days < 1 || days > 31) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.code(), "统计时间范围最多覆盖 31 个自然日");
        }
        return new QueryRange(startAt, endAt,
                startAt.toLocalDate().datesUntil(lastDate.plusDays(1)).toList());
    }

    private StatisticsResponses.Range responseRange(QueryRange range) {
        return new StatisticsResponses.Range(range.startAt(), range.endAt());
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String amount(BigDecimal value) {
        return money(value).toPlainString();
    }

    private record QueryRange(LocalDateTime startAt, LocalDateTime endAt, List<LocalDate> dates) {
    }
}
