package com.chareslm.shopping.search.dto;

import java.util.List;

/**
 * 热词结果。
 */
public record HotWordResponse(List<HotWord> words) {

    public record HotWord(String keyword, long count) {
    }
}
