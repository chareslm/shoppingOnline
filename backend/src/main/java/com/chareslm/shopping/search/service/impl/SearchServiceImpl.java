package com.chareslm.shopping.search.service.impl;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.product.dto.response.SpuResponse;
import com.chareslm.shopping.product.service.SpuService;
import com.chareslm.shopping.search.dto.HotWordResponse;
import com.chareslm.shopping.search.dto.ProductSearchItem;
import com.chareslm.shopping.search.dto.SearchRequest;
import com.chareslm.shopping.search.dto.SuggestResponse;
import com.chareslm.shopping.search.entity.SearchLog;
import com.chareslm.shopping.search.mapper.SearchLogMapper;
import com.chareslm.shopping.search.service.SearchIndexService;
import com.chareslm.shopping.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int HOT_WORD_DAYS = 7;

    private final SearchIndexService searchIndexService;
    private final SearchLogMapper searchLogMapper;
    private final SpuService spuService;

    @Override
    public PageResponse<ProductSearchItem> search(SearchRequest request, Long userId) {
        recordSearch(request.keyword(), userId);
        if (StringUtils.hasText(request.keyword())) {
            try {
                // ES 优先，失败回退 MySQL
                return searchIndexService.search(request);
            } catch (Exception e) {
                log.warn("ES search failed, fallback to MySQL. keyword={}", request.keyword(), e);
            }
        }
        return searchViaMysql(request);
    }

    @Override
    public SuggestResponse suggest(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new SuggestResponse(List.of());
        }
        String normalized = keyword.trim().toLowerCase();
        List<String> fromLog = searchLogMapper
                .selectSuggestions(normalized, HOT_WORD_DAYS, 10)
                .stream().map(SearchLogMapper.KeywordCount::keyword).toList();
        return new SuggestResponse(fromLog);
    }

    @Override
    public HotWordResponse hotWords(int limit) {
        List<HotWordResponse.HotWord> words = searchLogMapper
                .selectHotWords(HOT_WORD_DAYS, limit)
                .stream().map(k -> new HotWordResponse.HotWord(k.keyword(), k.cnt()))
                .toList();
        return new HotWordResponse(words);
    }

    @Override
    public void recordSearch(String keyword, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String normalized = keyword.trim().toLowerCase();
        if (normalized.length() > 128) {
            normalized = normalized.substring(0, 128);
        }
        SearchLog log = new SearchLog();
        log.setKeyword(normalized);
        log.setUserId(userId);
        try {
            searchLogMapper.insert(log);
        } catch (Exception e) {
            // 搜索日志失败不影响检索主流程
            logError("record search log failed", e);
        }
    }

    private PageResponse<ProductSearchItem> searchViaMysql(SearchRequest request) {
        PageResponse<SpuResponse> result = spuService.search(
                request.categoryId(), request.keyword(), request.brand(),
                request.priceMin(), request.priceMax(), request.sort(),
                request.page(), request.pageSize());
        List<ProductSearchItem> items = result.items().stream()
                .map(this::toItem).toList();
        return new PageResponse<>(items, result.total(), result.page(), result.pageSize());
    }

    private ProductSearchItem toItem(SpuResponse spu) {
        return new ProductSearchItem(spu.id(), spu.shopId(), spu.categoryId(), spu.brand(), spu.name(),
                spu.subtitle(), spu.mainImage(), spu.priceMin(), spu.priceMax(), spu.sales(), spu.rating());
    }

    private void logError(String message, Exception e) {
        log.warn("{}: {}", message, e.getMessage());
    }
}
