package com.chareslm.shopping.search.dto;

import java.util.List;

/**
 * 搜索建议结果。
 */
public record SuggestResponse(List<String> suggestions) {
}
