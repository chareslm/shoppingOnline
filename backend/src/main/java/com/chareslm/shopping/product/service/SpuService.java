package com.chareslm.shopping.product.service;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.product.dto.request.SkuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuAuditRequest;
import com.chareslm.shopping.product.dto.request.SpuCreateRequest;
import com.chareslm.shopping.product.dto.request.SpuStatusRequest;
import com.chareslm.shopping.product.dto.request.SpuUpdateRequest;
import com.chareslm.shopping.product.dto.response.SpuDetailResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;

public interface SpuService {

    SpuDetailResponse create(Long operatorId, SpuCreateRequest request);

    SpuDetailResponse update(Long operatorId, Long spuId, SpuUpdateRequest request);

    SpuDetailResponse getDetail(Long spuId);

    PageResponse<SpuResponse> page(Long categoryId, String keyword, String status, int page, int pageSize);

    /**
     * 商品检索（仅上架商品，支持关键词/类目/品牌/价格区间/排序）。
     * sort: DEFAULT / SALES_DESC / PRICE_ASC / PRICE_DESC / RATING_DESC / NEWEST。
     */
    PageResponse<SpuResponse> search(Long categoryId, String keyword, String brand,
                                     java.math.BigDecimal priceMin, java.math.BigDecimal priceMax,
                                     String sort, int page, int pageSize);

    /** 商家状态流转：SUBMIT 提交审核 / PUBLISH 上架 / OFF_SHELF 下架。 */
    SpuResponse changeStatus(Long operatorId, Long spuId, SpuStatusRequest request);

    /** 管理员审核：APPROVE 通过 / REJECT 驳回。 */
    SpuResponse audit(Long operatorId, Long spuId, SpuAuditRequest request);

    /** 为已有 SPU 追加 SKU。 */
    SpuDetailResponse addSku(Long operatorId, Long spuId, SkuCreateRequest request);

    /** 回写平均评分（评价模块调用）。 */
    void updateRating(Long spuId, java.math.BigDecimal rating);

    /** 按状态列出全部商品（搜索索引重建等内部调用）。 */
    java.util.List<SpuResponse> listByStatus(String status);

    /** 获取商品列表项（搜索索引等内部调用）。 */
    SpuResponse getSpu(Long spuId);
}
