package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SkuUpdateRequest(
        @Size(max = 64) String skuCode,
        String attributes,
        @Size(max = 512) String image,
        @NotNull @DecimalMin("0.01") BigDecimal price
) {
}
