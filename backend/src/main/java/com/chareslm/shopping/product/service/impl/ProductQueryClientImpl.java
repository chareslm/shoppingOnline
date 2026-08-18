package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.cart.client.ProductQueryClient;
import com.chareslm.shopping.cart.dto.ProductSkuView;
import com.chareslm.shopping.product.service.SkuQueryService;
import com.chareslm.shopping.product.service.SkuValidityInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * cart 模块 ProductQueryClient 的实现（被调用方 product 提供）。
 * <p>
 * 委托给本模块的 SkuQueryService 完成 SKU 可售性判定，并将内部模型
 * SkuValidityInfo 映射为 cart 侧的接口模型 ProductSkuView。
 */
@Service
@RequiredArgsConstructor
public class ProductQueryClientImpl implements ProductQueryClient {

    private final SkuQueryService skuQueryService;

    @Override
    public ProductSkuView getSkuSnapshot(Long skuId) {
        SkuValidityInfo info = skuQueryService.getSkuValidity(skuId);
        return new ProductSkuView(info.skuId(), info.spuId(), info.shopId(), info.skuName(),
                info.skuImage(), info.price(), info.onSale(), info.invalidReason());
    }
}