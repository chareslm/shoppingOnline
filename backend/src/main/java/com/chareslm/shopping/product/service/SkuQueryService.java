package com.chareslm.shopping.product.service;

/**
 * SKU 跨模块查询接口（购物车/交易模块校验商品有效性、价格与归属）。
 * <p>
 * 架构约定（docs/backend-package-architecture.md §6）：跨模块读取信息必须走
 * 模块暴露的 Service，禁止直接查表。cart/trade 通过本接口获取 SKU 可售状态、
 * 服务端售价与店铺归属，避免信任客户端提交的价格与商品数据。
 */
public interface SkuQueryService {

    /**
     * 查询 SKU 有效性（存在性 + 启用 + 所属 SPU 上架 + 店铺归属 + 服务端售价）。
     *
     * @param skuId SKU ID
     * @return 有效性信息；SKU 不存在时返回 onSale=false 且 invalidReason 说明原因
     */
    SkuValidityInfo getSkuValidity(Long skuId);
}