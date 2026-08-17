package com.chareslm.shopping.search.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.product.service.SpuService;
import com.chareslm.shopping.search.document.ProductSearchDocument;
import com.chareslm.shopping.search.dto.ProductSearchItem;
import com.chareslm.shopping.search.dto.SearchRequest;
import com.chareslm.shopping.search.repository.ProductSearchRepository;
import com.chareslm.shopping.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final SpuService spuService;

    @Override
    public void index(Long spuId) {
        try {
            SpuResponse spu = spuService.getSpu(spuId);
            if ("ON_SALE".equals(spu.status())) {
                productSearchRepository.save(toDocument(spu));
            } else {
                productSearchRepository.deleteById(spuId);
            }
        } catch (Exception e) {
            log.warn("index product to ES failed, spuId={}", spuId, e);
        }
    }

    @Override
    public void remove(Long spuId) {
        try {
            productSearchRepository.deleteById(spuId);
        } catch (Exception e) {
            log.warn("remove product from ES failed, spuId={}", spuId, e);
        }
    }

    @Override
    public int reindexAll() {
        List<SpuResponse> onSale = spuService.listByStatus("ON_SALE");
        int count = 0;
        for (SpuResponse spu : onSale) {
            try {
                productSearchRepository.save(toDocument(spu));
                count++;
            } catch (Exception e) {
                log.warn("reindex product failed, spuId={}", spu.id(), e);
            }
        }
        return count;
    }

    @Override
    public PageResponse<ProductSearchItem> search(SearchRequest request) {
        NativeQuery query = buildNativeQuery(request);
        SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(query, ProductSearchDocument.class);
        List<ProductSearchItem> items = new ArrayList<>();
        for (SearchHit<ProductSearchDocument> hit : hits) {
            items.add(toItem(hit.getContent()));
        }
        return new PageResponse<>(items, hits.getTotalHits(), request.page(), request.pageSize());
    }

    private NativeQuery buildNativeQuery(SearchRequest request) {
        NativeQueryBuilder builder = NativeQuery.builder();
        boolean hasQuery = StringUtils.hasText(request.keyword())
                || request.categoryId() != null
                || StringUtils.hasText(request.brand())
                || request.priceMin() != null
                || request.priceMax() != null;

        if (hasQuery) {
            builder.withQuery(q -> q.bool(b -> {
                if (StringUtils.hasText(request.keyword())) {
                    b.must(m -> m.multiMatch(mm -> mm
                            .query(request.keyword())
                            .fields("name", "subtitle", "brand")));
                }
                if (request.categoryId() != null) {
                    b.filter(f -> f.term(t -> t.field("categoryId").value(request.categoryId())));
                }
                if (StringUtils.hasText(request.brand())) {
                    b.filter(f -> f.term(t -> t.field("brand").value(request.brand())));
                }
                if (request.priceMin() != null) {
                    b.filter(f -> f.range(r -> r.number(n -> n
                            .field("priceMin").gte(request.priceMin().doubleValue()))));
                }
                if (request.priceMax() != null) {
                    b.filter(f -> f.range(r -> r.number(n -> n
                            .field("priceMin").lte(request.priceMax().doubleValue()))));
                }
                return b;
            }));
        }
        builder.withPageable(PageRequest.of(request.page() - 1, request.pageSize()));
        applySort(builder, request.sort());
        return builder.build();
    }

    private void applySort(NativeQueryBuilder builder, String sort) {
        if (sort == null || sort.isBlank() || "DEFAULT".equals(sort)) {
            return;
        }
        String field = switch (sort) {
            case "SALES_DESC" -> "sales";
            case "PRICE_ASC", "PRICE_DESC" -> "priceMin";
            case "RATING_DESC" -> "rating";
            default -> null;
        };
        if (field == null) {
            return;
        }
        SortOrder order = "PRICE_ASC".equals(sort) ? SortOrder.Asc : SortOrder.Desc;
        builder.withSort(s -> s.field(f -> f.field(field).order(order)));
    }

    private ProductSearchDocument toDocument(SpuResponse spu) {
        ProductSearchDocument doc = new ProductSearchDocument();
        doc.setSpuId(spu.id());
        doc.setShopId(spu.shopId());
        doc.setCategoryId(spu.categoryId());
        doc.setName(spu.name());
        doc.setSubtitle(spu.subtitle());
        doc.setBrand(spu.brand());
        doc.setPriceMin(spu.priceMin());
        doc.setPriceMax(spu.priceMax());
        doc.setSales(spu.sales());
        doc.setRating(spu.rating());
        doc.setTags(List.of());
        doc.setStatus(spu.status());
        return doc;
    }

    private ProductSearchItem toItem(ProductSearchDocument doc) {
        return new ProductSearchItem(doc.getSpuId(), doc.getShopId(), doc.getCategoryId(), doc.getBrand(),
                doc.getName(), doc.getSubtitle(), null, doc.getPriceMin(), doc.getPriceMax(),
                doc.getSales(), doc.getRating());
    }
}
