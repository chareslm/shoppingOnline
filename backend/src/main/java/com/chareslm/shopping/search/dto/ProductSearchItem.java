package com.chareslm.shopping.search.dto;

import java.math.BigDecimal;

/**
 * 商品检索结果项。
 */
public record ProductSearchItem(
        Long spuId,
        Long shopId,
        Long categoryId,
        String brand,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer sales,
        BigDecimal rating
) {
}
