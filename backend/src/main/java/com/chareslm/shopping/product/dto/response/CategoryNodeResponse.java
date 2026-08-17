package com.chareslm.shopping.product.dto.response;

import java.util.List;

/**
 * 类目树节点。
 */
public record CategoryNodeResponse(
        Long id,
        Long parentId,
        String name,
        Integer level,
        Integer sortOrder,
        String icon,
        Integer status,
        List<CategoryNodeResponse> children
) {
}
