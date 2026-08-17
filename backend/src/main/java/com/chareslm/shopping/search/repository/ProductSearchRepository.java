package com.chareslm.shopping.search.repository;

import com.chareslm.shopping.search.document.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品搜索索引仓库（Elasticsearch）。
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, Long> {
}
