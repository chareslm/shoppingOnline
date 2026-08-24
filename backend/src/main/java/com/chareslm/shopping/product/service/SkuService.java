package com.chareslm.shopping.product.service;

import com.chareslm.shopping.product.dto.request.SkuStockAdjustRequest;
import com.chareslm.shopping.product.dto.request.SkuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SkuResponse;

public interface SkuService {

    SkuResponse update(Long operatorId, Long skuId, SkuUpdateRequest request);

    SkuResponse adjustStock(Long operatorId, Long skuId, SkuStockAdjustRequest request);

    SkuResponse get(Long operatorId, Long skuId);
}
