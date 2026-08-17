package com.chareslm.shopping.product.dto.response;

import java.math.BigDecimal;

/**
 * SPU 列表项（不携带详情，用于分页/搜索展示）。
 */
public record SpuResponse(
        Long id,
        Long shopId,
        Long categoryId,
        String brand,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer sales,
        BigDecimal rating,
        String status
) {
}
