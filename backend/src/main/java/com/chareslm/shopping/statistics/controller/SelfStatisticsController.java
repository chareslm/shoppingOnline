package com.chareslm.shopping.statistics.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.security.context.CurrentUser;
import com.chareslm.shopping.statistics.dto.response.StatisticsResponses;
import com.chareslm.shopping.statistics.service.StatisticsQueryService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users/me/statistics")
@Validated
public class SelfStatisticsController {
    private final StatisticsQueryService statisticsQueryService;

    public SelfStatisticsController(StatisticsQueryService statisticsQueryService) {
        this.statisticsQueryService = statisticsQueryService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('statistics:self:view')")
    public ApiResponse<StatisticsResponses.UserOverview> overview(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(defaultValue = "Asia/Shanghai") String timezone,
            @RequestParam(defaultValue = "DAY") String granularity) {
        return ApiResponse.success(statisticsQueryService.getUserOverview(CurrentUser.require().userId(),
                startAt, endAt, timezone, granularity));
    }
}
