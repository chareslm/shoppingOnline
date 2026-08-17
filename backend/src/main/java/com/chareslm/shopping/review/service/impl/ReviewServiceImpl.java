package com.chareslm.shopping.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.product.service.SpuService;
import com.chareslm.shopping.review.dto.request.ReviewAuditRequest;
import com.chareslm.shopping.review.dto.request.ReviewCreateRequest;
import com.chareslm.shopping.review.dto.request.ReviewReplyRequest;
import com.chareslm.shopping.review.dto.response.ReviewResponse;
import com.chareslm.shopping.review.dto.response.ReviewStatsResponse;
import com.chareslm.shopping.review.entity.Review;
import com.chareslm.shopping.review.entity.ReviewReply;
import com.chareslm.shopping.review.enums.ReviewStatus;
import com.chareslm.shopping.review.mapper.ReviewMapper;
import com.chareslm.shopping.review.mapper.ReviewReplyMapper;
import com.chareslm.shopping.review.service.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** 交易模块已完成订单状态码（成员4 order.status = 3）。 */
    private static final int ORDER_COMPLETED = 3;

    private final ReviewMapper reviewMapper;
    private final ReviewReplyMapper reviewReplyMapper;
    private final SpuService spuService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReviewResponse create(Long userId, ReviewCreateRequest request) {
        ReviewMapper.OrderItemContext context = reviewMapper.selectOrderItemContext(request.orderItemId());
        if (context == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
        if (!context.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
        if (context.orderStatus() != ORDER_COMPLETED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
        long existing = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderItemId, request.orderItemId()));
        if (existing > 0) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setOrderId(context.orderId());
        review.setOrderItemId(context.orderItemId());
        review.setSpuId(context.spuId());
        review.setSkuId(context.skuId());
        review.setUserId(userId);
        review.setShopId(context.shopId());
        review.setRating(request.rating());
        review.setContent(trimToNull(request.content()));
        review.setImages(toJson(request.images()));
        review.setIsAnonymous(Boolean.TRUE.equals(request.anonymous()) ? 1 : 0);
        review.setStatus(ReviewStatus.DISPLAYED.name());
        reviewMapper.insert(review);

        refreshSpuRating(context.spuId());
        return toResponse(review, null);
    }

    @Override
    public PageResponse<ReviewResponse> listBySpu(Long spuId, int page, int pageSize) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getSpuId, spuId)
                        .eq(Review::getStatus, ReviewStatus.DISPLAYED.name())
                        .orderByDesc(Review::getCreatedAt));
        List<Long> reviewIds = result.getRecords().stream().map(Review::getId).toList();
        Map<Long, String> replies = reviewIds.isEmpty() ? Map.of()
                : reviewReplyMapper.selectList(new LambdaQueryWrapper<ReviewReply>()
                        .in(ReviewReply::getReviewId, reviewIds))
                .stream().collect(Collectors.toMap(ReviewReply::getReviewId, ReviewReply::getContent));
        List<ReviewResponse> items = result.getRecords().stream()
                .map(r -> toResponse(r, replies.get(r.getId())))
                .toList();
        return new PageResponse<>(items, result.getTotal(), page, pageSize);
    }

    @Override
    public ReviewStatsResponse stats(Long spuId) {
        ReviewMapper.RatingAggregate aggregate = reviewMapper.selectRatingAggregate(spuId);
        if (aggregate == null || aggregate.totalCount() == 0) {
            return new ReviewStatsResponse(BigDecimal.ZERO, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO);
        }
        long total = aggregate.totalCount();
        long positive = aggregate.fiveStar() + aggregate.fourStar();
        BigDecimal positiveRate = BigDecimal.valueOf(positive)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        BigDecimal average = aggregate.averageRating().setScale(2, RoundingMode.HALF_UP);
        return new ReviewStatsResponse(average, total, aggregate.fiveStar(), aggregate.fourStar(),
                aggregate.threeStar(), aggregate.twoStar(), aggregate.oneStar(), positiveRate);
    }

    @Override
    @Transactional
    public ReviewResponse reply(Long operatorId, Long reviewId, ReviewReplyRequest request) {
        Review review = requireReview(reviewId);
        ReviewReply existing = reviewReplyMapper.selectOne(
                new LambdaQueryWrapper<ReviewReply>().eq(ReviewReply::getReviewId, reviewId));
        if (existing != null) {
            existing.setContent(request.content().trim());
            existing.setRepliedBy(operatorId);
            reviewReplyMapper.updateById(existing);
            return toResponse(review, existing.getContent());
        }
        ReviewReply reply = new ReviewReply();
        reply.setReviewId(reviewId);
        reply.setShopId(review.getShopId());
        reply.setContent(request.content().trim());
        reply.setRepliedBy(operatorId);
        reviewReplyMapper.insert(reply);
        return toResponse(review, reply.getContent());
    }

    @Override
    @Transactional
    public void audit(Long reviewId, ReviewAuditRequest request) {
        Review review = requireReview(reviewId);
        review.setStatus("HIDE".equals(request.action()) ? ReviewStatus.HIDDEN.name() : ReviewStatus.DISPLAYED.name());
        reviewMapper.updateById(review);
        refreshSpuRating(review.getSpuId());
    }

    private void refreshSpuRating(Long spuId) {
        try {
            ReviewStatsResponse stats = stats(spuId);
            spuService.updateRating(spuId, stats.averageRating());
        } catch (Exception e) {
            log.warn("refresh spu rating failed, spuId={}", spuId, e);
        }
    }

    private Review requireReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private ReviewResponse toResponse(Review review, String reply) {
        return new ReviewResponse(review.getId(), review.getSpuId(), review.getSkuId(), review.getUserId(),
                review.getRating(), review.getContent(), fromJson(review.getImages()),
                review.getIsAnonymous() != null && review.getIsAnonymous() == 1,
                review.getCreatedAt(), reply);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("serialize review images failed", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("deserialize review images failed", e);
            return List.of();
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
