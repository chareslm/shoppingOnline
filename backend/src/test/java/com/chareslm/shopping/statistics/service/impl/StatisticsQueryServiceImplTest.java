package com.chareslm.shopping.statistics.service.impl;

import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.merchant.entity.Shop;
import com.chareslm.shopping.merchant.service.MerchantShopQueryService;
import com.chareslm.shopping.statistics.mapper.StatisticsMapper;
import com.chareslm.shopping.statistics.mapper.model.PlatformOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.PlatformTrendRow;
import com.chareslm.shopping.statistics.mapper.model.ShopOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.UserOverviewRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsQueryServiceImplTest {
    private StatisticsMapper mapper;
    private MerchantShopQueryService merchantShopQueryService;
    private StatisticsQueryServiceImpl service;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @BeforeEach
    void setUp() {
        mapper = mock(StatisticsMapper.class);
        merchantShopQueryService = mock(MerchantShopQueryService.class);
        service = new StatisticsQueryServiceImpl(mapper, merchantShopQueryService);
        startAt = LocalDateTime.of(2026, 8, 1, 0, 0);
        endAt = LocalDateTime.of(2026, 8, 3, 0, 0);
    }

    @Test
    void platformOverviewPreservesGrossPaidAmountAndCalculatesNetActivity() {
        PlatformOverviewRow row = new PlatformOverviewRow();
        row.setNewUserCount(3L);
        row.setActiveUserCount(9L);
        row.setPaidOrderCount(2L);
        row.setPaidBuyerCount(2L);
        row.setGrossPaidAmount(new BigDecimal("150.00"));
        row.setSuccessfulRefundAmount(new BigDecimal("180.00"));
        row.setOnSaleProductCount(4L);
        row.setSearchCount(7L);
        row.setDisplayedReviewCount(1L);
        when(mapper.selectPlatformOverview(startAt, endAt)).thenReturn(row);

        var result = service.getPlatformOverview(startAt, endAt, "Asia/Shanghai", "DAY");

        assertEquals("v1", result.metricVersion());
        assertEquals("150.00", result.metrics().grossPaidAmount());
        assertEquals("180.00", result.metrics().successfulRefundAmount());
        assertEquals("-30.00", result.metrics().netCashflowActivity());
        assertEquals(3, result.metrics().newUsers());
    }

    @Test
    void trendsFillMissingBusinessDatesWithZeros() {
        PlatformTrendRow secondDay = new PlatformTrendRow();
        secondDay.setMetricDate(LocalDate.of(2026, 8, 2));
        secondDay.setPaidOrderCount(1L);
        secondDay.setGrossPaidAmount(new BigDecimal("20.00"));
        secondDay.setSuccessfulRefundAmount(new BigDecimal("30.00"));
        when(mapper.selectPlatformTrends(startAt, endAt)).thenReturn(List.of(secondDay));

        var result = service.getPlatformTrends(startAt, endAt, null, null);

        assertEquals(2, result.points().size());
        assertEquals(LocalDate.of(2026, 8, 1), result.points().get(0).date());
        assertEquals("0.00", result.points().get(0).grossPaidAmount());
        assertEquals("-10.00", result.points().get(1).netCashflowActivity());
    }

    @Test
    void shopOverviewUsesOwnedShopAndReturnsNullForEmptyAverage() {
        Shop shop = new Shop();
        shop.setId(88L);
        shop.setName("统计店铺");
        shop.setStatus("OPEN");
        when(merchantShopQueryService.requireOpenOwnedShop(7L)).thenReturn(shop);
        when(mapper.selectShopOverview(88L, startAt, endAt)).thenReturn(new ShopOverviewRow());

        var result = service.getShopOverview(7L, startAt, endAt, "Asia/Shanghai", "day");

        assertEquals(88L, result.shopId());
        assertEquals("统计店铺", result.shopName());
        assertNull(result.metrics().averageOrderValue());
        assertNull(result.metrics().averageRating());
        verify(merchantShopQueryService).requireOpenOwnedShop(7L);
    }

    @Test
    void invalidTimezoneAndRangesAreRejectedBeforeQuerying() {
        assertThrows(BusinessException.class,
                () -> service.getPlatformOverview(startAt, endAt, "UTC", "DAY"));
        assertThrows(BusinessException.class,
                () -> service.getPlatformOverview(startAt, startAt, "Asia/Shanghai", "DAY"));
        assertThrows(BusinessException.class,
                () -> service.getPlatformOverview(startAt, startAt.plusDays(32), "Asia/Shanghai", "DAY"));
    }

    @Test
    void userOverviewOnlyUsesAuthenticatedUserScopeAndPreservesCrossPeriodRefund() {
        UserOverviewRow row = new UserOverviewRow();
        row.setPaidOrderCount(2L);
        row.setGrossPaidAmount(new BigDecimal("150.00"));
        row.setSuccessfulRefundAmount(new BigDecimal("180.00"));
        row.setDisplayedReviewCount(1L);
        when(mapper.selectUserOverview(7L, startAt, endAt)).thenReturn(row);

        var result = service.getUserOverview(7L, startAt, endAt, "Asia/Shanghai", "DAY");

        assertEquals(2, result.metrics().paidOrderCount());
        assertEquals("150.00", result.metrics().grossPaidAmount());
        assertEquals("180.00", result.metrics().successfulRefundAmount());
        assertEquals(1, result.metrics().displayedReviewCount());
        verify(mapper).selectUserOverview(7L, startAt, endAt);
    }
}
