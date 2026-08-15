package com.chareslm.shopping.search.controller;

import com.chareslm.shopping.common.api.ApiResponse;
import com.chareslm.shopping.search.service.SearchIndexService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
public class SearchAdminController {

    private final SearchIndexService searchIndexService;

    public SearchAdminController(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    /** 全量重建商品搜索索引（管理员）。 */
    @PostMapping("/reindex")
    @PreAuthorize("hasAuthority('product:audit')")
    public ApiResponse<Map<String, Object>> reindex() {
        int count = searchIndexService.reindexAll();
        return ApiResponse.success(Map.of("indexed", count));
    }
}
