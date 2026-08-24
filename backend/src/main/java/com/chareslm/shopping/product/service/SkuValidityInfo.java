package com.chareslm.shopping.product.service;

import java.math.BigDecimal;

/**
 * SKU 跨模块有效性查询结果（购物车/交易模块调用）。
 * <p>
 * 架构约定：DTO 不跨模块直接复用，跨模块参数定义为清晰的接口模型。
 * 本 record 即 cart/trade 模块读取商品信息的接口模型。
 */
public record SkuValidityInfo(
        Long skuId,
        Long spuId,
        Long shopId,
        /** SPU 商品名称 */
        String skuName,
        /** SKU 图（无则回退 SPU 主图） */
        String skuImage,
        /** 服务端当前售价 */
        BigDecimal price,
        /** 是否可售：SKU 启用（status=1）且所属 SPU 上架（ON_SALE） */
        boolean onSale,
        /** 不可售原因（onSale=false 时非空） */
        String invalidReason
) {
}