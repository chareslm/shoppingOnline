package com.chareslm.shopping.product.event;

/**
 * 商品变更事件（用于搜索索引同步等最终一致场景）。
 */
public record ProductChangedEvent(Long spuId, String status) {
}
