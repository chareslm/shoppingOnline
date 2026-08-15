package com.chareslm.shopping.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long spuId,
        Long skuId,
        Long userId,
        Integer rating,
        String content,
        List<String> images,
        Boolean anonymous,
        LocalDateTime createdAt,
        String reply
) {
}
