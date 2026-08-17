package com.chareslm.shopping.review.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.review.dto.request.ReviewAuditRequest;
import com.chareslm.shopping.review.dto.request.ReviewReplyRequest;
import com.chareslm.shopping.review.dto.response.ReviewResponse;
import com.chareslm.shopping.review.service.ReviewService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewAdminController {

    private final ReviewService reviewService;

    public ReviewAdminController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** 商家回复评价。 */
    @PutMapping("/merchant/review/{reviewId}/reply")
    @PreAuthorize("hasAuthority('review:reply')")
    public ApiResponse<ReviewResponse> reply(@PathVariable Long reviewId,
                                             @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.success(reviewService.reply(CurrentUser.require().userId(), reviewId, request));
    }

    /** 管理员审核评价（隐藏/恢复）。 */
    @PutMapping("/admin/review/{reviewId}/audit")
    @PreAuthorize("hasAuthority('review:audit')")
    public ApiResponse<Void> audit(@PathVariable Long reviewId,
                                   @Valid @RequestBody ReviewAuditRequest request) {
        reviewService.audit(reviewId, request);
        return ApiResponse.success(null);
    }
}
