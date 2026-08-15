package com.chareslm.shopping.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.search.entity.SearchLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SearchLogMapper extends BaseMapper<SearchLog> {

    /**
     * 近 N 天热词统计（按出现次数降序）。
     */
    @Select("""
            SELECT keyword, COUNT(*) AS cnt
            FROM search_log
            WHERE created_at >= NOW() - INTERVAL #{days} DAY
            GROUP BY keyword
            ORDER BY cnt DESC, keyword ASC
            LIMIT #{limit}
            """)
    List<KeywordCount> selectHotWords(@Param("days") int days, @Param("limit") int limit);

    /**
     * 关键词前缀建议（近 N 天出现过的关键词，按频次降序）。
     */
    @Select("""
            SELECT keyword, COUNT(*) AS cnt
            FROM search_log
            WHERE created_at >= NOW() - INTERVAL #{days} DAY
              AND keyword LIKE CONCAT(#{prefix}, '%')
            GROUP BY keyword
            ORDER BY cnt DESC
            LIMIT #{limit}
            """)
    List<KeywordCount> selectSuggestions(@Param("prefix") String prefix, @Param("days") int days, @Param("limit") int limit);

    record KeywordCount(String keyword, long cnt) {
    }
}
