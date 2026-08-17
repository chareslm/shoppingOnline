package com.chareslm.shopping.product.dto.response;

import java.math.BigDecimal;

public record SkuResponse(
        Long id,
        Long spuId,
        String skuCode,
        String attributes,
        String image,
        BigDecimal price,
        Integer availableStock,
        Integer reservedStock,
        Integer soldStock,
        Integer status
) {
}
