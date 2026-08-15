package com.chareslm.shopping.product.dto.response;

public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        Integer level,
        Integer sortOrder,
        String icon,
        Integer status
) {
}
