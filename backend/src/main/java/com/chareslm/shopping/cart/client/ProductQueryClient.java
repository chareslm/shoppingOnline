package com.chareslm.shopping.cart.client;

import com.chareslm.shopping.cart.dto.ProductSkuView;

/**
 * 商品 SKU 跨模块查询接口（购物车/交易模块校验商品有效性、价格与归属）。
 * <p>
 * 架构约定（docs/backend-package-architecture.md §3/§4）：跨模块调用只允许通过
 * 对方模块的 client 接口，接口定义在调用方（cart），实现在被调用方（product）。
 * 返回服务端权威价格与可售状态，避免信任客户端提交的价格与商品数据。
 */
public interface ProductQueryClient {

    /**
     * 返回商品 SKU 快照（存在性 + 启用 + 所属 SPU 上架 + 店铺归属 + 服务端售价）。
     *
     * @param skuId SKU ID
     * @return SKU 快照；SKU 不存在时返回 onSale=false 且 invalidReason 说明原因
     */
    ProductSkuView getSkuSnapshot(Long skuId);
}