package com.chareslm.shopping.product.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 库存调整。
 * change 为正表示增加可售库存，为负表示减少可售库存（减少后不得小于 0）。
 */
public record SkuStockAdjustRequest(
        @NotNull Integer change,
        String remark
) {
}
