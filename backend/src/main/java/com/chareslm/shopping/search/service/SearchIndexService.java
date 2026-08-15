package com.chareslm.shopping.search.service;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.search.dto.ProductSearchItem;
import com.chareslm.shopping.search.dto.SearchRequest;

import java.util.List;

/**
 * 搜索索引服务：MySQL → Elasticsearch 同步与检索。
 * <p>
 * 所有 ES 操作降级为 try/catch，ES 不可用时检索回退 MySQL，索引同步静默失败。
 */
public interface SearchIndexService {

    /** 索引单个商品（上架则写索引，否则删除索引）。 */
    void index(Long spuId);

    /** 从索引删除商品。 */
    void remove(Long spuId);

    /** 全量重建索引（管理员触发）。 */
    int reindexAll();

    /** 通过 ES 检索商品。ES 不可用或查询失败时抛出异常，由调用方回退 MySQL。 */
    PageResponse<ProductSearchItem> search(SearchRequest request);
}
