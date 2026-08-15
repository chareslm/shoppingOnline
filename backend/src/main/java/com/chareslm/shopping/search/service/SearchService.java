package com.chareslm.shopping.search.service;

import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.search.dto.HotWordResponse;
import com.chareslm.shopping.search.dto.ProductSearchItem;
import com.chareslm.shopping.search.dto.SearchRequest;
import com.chareslm.shopping.search.dto.SuggestResponse;

public interface SearchService {

    PageResponse<ProductSearchItem> search(SearchRequest request, Long userId);

    SuggestResponse suggest(String keyword);

    HotWordResponse hotWords(int limit);

    void recordSearch(String keyword, Long userId);
}
