package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpuUpdateRequest(
        @NotNull Long categoryId,
        @Size(max = 64) String brand,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String subtitle,
        @Size(max = 512) String mainImage,
        java.util.List<String> images,
        String detail
) {
}
