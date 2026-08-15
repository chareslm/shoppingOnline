package com.chareslm.shopping.search.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.common.api.PageResponse;
import com.chareslm.shopping.search.dto.HotWordResponse;
import com.chareslm.shopping.search.dto.ProductSearchItem;
import com.chareslm.shopping.search.dto.SearchRequest;
import com.chareslm.shopping.search.dto.SuggestResponse;
import com.chareslm.shopping.search.service.SearchService;
import com.chareslm.shopping.security.context.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search")
@Validated
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 商品检索（公开）。 */
    @GetMapping
    public ApiResponse<PageResponse<ProductSearchItem>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int pageSize) {
        SearchRequest request = new SearchRequest(keyword, categoryId, brand, priceMin, priceMax,
                sort, page, pageSize);
        return ApiResponse.success(searchService.search(request, currentUserIdOrNull()));
    }

    /** 搜索建议（公开）。 */
    @GetMapping("/suggest")
    public ApiResponse<SuggestResponse> suggest(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(searchService.suggest(keyword));
    }

    /** 搜索热词（公开）。 */
    @GetMapping("/hot-words")
    public ApiResponse<HotWordResponse> hotWords(@RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.success(searchService.hotWords(limit));
    }

    private Long currentUserIdOrNull() {
        try {
            return CurrentUser.require().userId();
        } catch (Exception e) {
            return null;
        }
    }
}
