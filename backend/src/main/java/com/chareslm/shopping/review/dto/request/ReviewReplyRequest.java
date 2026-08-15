package com.chareslm.shopping.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
