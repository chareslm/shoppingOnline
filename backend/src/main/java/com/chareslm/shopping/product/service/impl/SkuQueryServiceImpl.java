package com.chareslm.shopping.product.service.impl;

import com.chareslm.shopping.product.entity.Sku;
import com.chareslm.shopping.product.entity.Spu;
import com.chareslm.shopping.product.enums.SpuStatus;
import com.chareslm.shopping.product.mapper.SkuMapper;
import com.chareslm.shopping.product.mapper.SpuMapper;
import com.chareslm.shopping.product.service.SkuQueryService;
import com.chareslm.shopping.product.service.SkuValidityInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SKU 跨模块查询实现。
 * <p>
 * 可售判定：SKU 存在且启用（status=1）且所属 SPU 状态为 ON_SALE（上架）。
 * 返回服务端当前售价与店铺归属，供购物车/交易模块做价格与有效性校验。
 */
@Service
@RequiredArgsConstructor
public class SkuQueryServiceImpl implements SkuQueryService {

    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;

    @Override
    public SkuValidityInfo getSkuValidity(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            return new SkuValidityInfo(skuId, null, null, null, null, null, false, "SKU 不存在");
        }
        if (sku.getStatus() == null || sku.getStatus() != 1) {
            return new SkuValidityInfo(skuId, sku.getSpuId(), null, null, sku.getImage(),
                    sku.getPrice(), false, "SKU 已停用");
        }
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) {
            return new SkuValidityInfo(skuId, sku.getSpuId(), null, null, sku.getImage(),
                    sku.getPrice(), false, "所属商品不存在");
        }
        if (!SpuStatus.ON_SALE.name().equals(spu.getStatus())) {
            return new SkuValidityInfo(skuId, sku.getSpuId(), spu.getShopId(), spu.getName(),
                    sku.getImage() != null ? sku.getImage() : spu.getMainImage(),
                    sku.getPrice(), false, "商品未上架");
        }
        return new SkuValidityInfo(skuId, sku.getSpuId(), spu.getShopId(), spu.getName(),
                sku.getImage() != null ? sku.getImage() : spu.getMainImage(),
                sku.getPrice(), true, null);
    }
}