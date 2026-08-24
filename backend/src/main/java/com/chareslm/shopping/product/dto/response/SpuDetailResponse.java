package com.chareslm.shopping.product.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU 详情（含 SKU 列表）。
 */
public record SpuDetailResponse(
        Long id,
        Long shopId,
        String shopName,
        Long categoryId,
        String categoryName,
        String brand,
        String name,
        String subtitle,
        String mainImage,
        List<String> images,
        String detail,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer sales,
        BigDecimal rating,
        String status,
        String auditRemark,
        LocalDateTime createdAt,
        List<SkuResponse> skus
) {
}
