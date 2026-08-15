package com.chareslm.shopping.review.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.review.dto.request.ReviewCreateRequest;
import com.chareslm.shopping.review.dto.response.ReviewResponse;
import com.chareslm.shopping.review.dto.response.ReviewStatsResponse;
import com.chareslm.shopping.review.service.ReviewService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 提交评价（需登录，且订单已完成）。 */
    @PostMapping
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.create(CurrentUser.require().userId(), request));
    }

    /** 商品评价列表（公开）。 */
    @GetMapping("/spu/{spuId}")
    public ApiResponse<PageResponse<ReviewResponse>> listBySpu(
            @PathVariable Long spuId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.success(reviewService.listBySpu(spuId, page, pageSize));
    }

    /** 商品评分聚合（公开）。 */
    @GetMapping("/spu/{spuId}/stats")
    public ApiResponse<ReviewStatsResponse> stats(@PathVariable Long spuId) {
        return ApiResponse.success(reviewService.stats(spuId));
    }
}
