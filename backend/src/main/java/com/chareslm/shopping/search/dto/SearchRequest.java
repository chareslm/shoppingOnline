package com.chareslm.shopping.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/**
 * 商品检索请求。
 * sort: DEFAULT / SALES_DESC / PRICE_ASC / PRICE_DESC / RATING_DESC / NEWEST。
 */
public record SearchRequest(
        String keyword,
        Long categoryId,
        String brand,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String sort,
        @Min(1) int page,
        @Min(1) @Max(50) int pageSize
) {
    public SearchRequest {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
    }
}
