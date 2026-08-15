package com.chareslm.shopping.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpuCreateRequest(
        @NotNull Long shopId,
        @NotNull Long categoryId,
        @Size(max = 64) String brand,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String subtitle,
        @Size(max = 512) String mainImage,
        List<String> images,
        String detail,
        @NotEmpty @Valid List<SkuCreateRequest> skus
) {
}
