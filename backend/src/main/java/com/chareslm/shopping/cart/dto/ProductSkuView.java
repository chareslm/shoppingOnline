package com.chareslm.shopping.cart.dto;

import java.math.BigDecimal;

/**
 * 商品 SKU 快照（跨模块接口模型，由 product 模块实现 ProductQueryClient 时填充）。
 * <p>
 * 架构约定：DTO 不跨模块直接复用，跨模块参数定义为清晰的接口模型。
 * 本 record 即 cart/trade 模块读取商品信息的接口模型。
 */
public record ProductSkuView(
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