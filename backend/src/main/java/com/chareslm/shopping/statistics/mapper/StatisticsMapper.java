package com.chareslm.shopping.statistics.mapper;

import com.chareslm.shopping.statistics.mapper.model.PlatformOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.PlatformTrendRow;
import com.chareslm.shopping.statistics.mapper.model.ShopOverviewRow;
import com.chareslm.shopping.statistics.mapper.model.ShopTrendRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsMapper {
    PlatformOverviewRow selectPlatformOverview(@Param("startAt") LocalDateTime startAt,
                                               @Param("endAt") LocalDateTime endAt);

    List<PlatformTrendRow> selectPlatformTrends(@Param("startAt") LocalDateTime startAt,
                                                @Param("endAt") LocalDateTime endAt);

    ShopOverviewRow selectShopOverview(@Param("shopId") Long shopId,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);

    List<ShopTrendRow> selectShopTrends(@Param("shopId") Long shopId,
                                        @Param("startAt") LocalDateTime startAt,
                                        @Param("endAt") LocalDateTime endAt);
}
