package com.chareslm.shopping.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpuCreateRequest(
        Long shopId,
        @NotBlank String categoryId,
        @Size(max = 64) String brand,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String subtitle,
        @Size(max = 1024) String mainImage,
        List<String> images,
        String detail,
        @NotEmpty List<@Valid SkuCreateRequest> skus
) {
}
