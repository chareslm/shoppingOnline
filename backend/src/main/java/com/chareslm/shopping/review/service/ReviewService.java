package com.chareslm.shopping.review.service;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.review.dto.request.ReviewAuditRequest;
import com.chareslm.shopping.review.dto.request.ReviewCreateRequest;
import com.chareslm.shopping.review.dto.request.ReviewReplyRequest;
import com.chareslm.shopping.review.dto.response.ReviewResponse;
import com.chareslm.shopping.review.dto.response.ReviewStatsResponse;

public interface ReviewService {

    ReviewResponse create(Long userId, ReviewCreateRequest request);

    PageResponse<ReviewResponse> listBySpu(Long spuId, int page, int pageSize);

    ReviewStatsResponse stats(Long spuId);

    ReviewResponse reply(Long operatorId, Long reviewId, ReviewReplyRequest request);

    void audit(Long reviewId, ReviewAuditRequest request);
}
