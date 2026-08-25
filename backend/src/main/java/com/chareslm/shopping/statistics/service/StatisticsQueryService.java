package com.chareslm.shopping.statistics.service;

import com.chareslm.shopping.statistics.dto.response.StatisticsResponses;

import java.time.LocalDateTime;

public interface StatisticsQueryService {
    StatisticsResponses.PlatformOverview getPlatformOverview(LocalDateTime startAt, LocalDateTime endAt,
                                                              String timezone, String granularity);

    StatisticsResponses.PlatformTrends getPlatformTrends(LocalDateTime startAt, LocalDateTime endAt,
                                                          String timezone, String granularity);

    StatisticsResponses.ShopOverview getShopOverview(Long userId, LocalDateTime startAt, LocalDateTime endAt,
                                                      String timezone, String granularity);

    StatisticsResponses.ShopTrends getShopTrends(Long userId, LocalDateTime startAt, LocalDateTime endAt,
                                                  String timezone, String granularity);
}
