package com.chareslm.shopping.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReviewCreateRequest(
        @NotNull Long orderItemId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String content,
        List<String> images,
        Boolean anonymous
) {
}
