package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.chareslm.shopping.product.dto.request.SkuStockAdjustRequest;
import com.chareslm.shopping.product.dto.request.SkuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SkuResponse;
import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final SkuMapper skuMapper;

    @Override
    @Transactional
    public SkuResponse update(Long operatorId, Long skuId, SkuUpdateRequest request) {
        Sku sku = requireSku(skuId);
        sku.setSkuCode(trimToNull(request.skuCode()));
        sku.setAttributes(trimToNull(request.attributes()));
        sku.setImage(trimToNull(request.image()));
        sku.setPrice(request.price());
        skuMapper.updateById(sku);
        return toResponse(sku);
    }

    @Override
    @Transactional
    public SkuResponse adjustStock(Long operatorId, Long skuId, SkuStockAdjustRequest request) {
        Sku sku = requireSku(skuId);
        int change = request.change();
        if (change == 0) {
            return toResponse(sku);
        }
        // 使用条件 UPDATE 原子调整，避免并发覆盖
        int rows = change > 0
                ? skuMapper.increaseAvailableStock(skuId, change)
                : skuMapper.reduceAvailableStock(skuId, -change);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        return toResponse(requireSku(skuId));
    }

    @Override
    public SkuResponse get(Long skuId) {
        return toResponse(requireSku(skuId));
    }

    private Sku requireSku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
        }
        return sku;
    }

    private static SkuResponse toResponse(Sku sku) {
        return new SkuResponse(sku.getId(), sku.getSpuId(), sku.getSkuCode(), sku.getAttributes(), sku.getImage(),
                sku.getPrice(), sku.getAvailableStock(), sku.getReservedStock(), sku.getSoldStock(), sku.getStatus());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
