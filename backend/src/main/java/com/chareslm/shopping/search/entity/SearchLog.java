package com.chareslm.shopping.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 搜索日志（热词统计）。
 */
@Getter
@Setter
@TableName("search_log")
public class SearchLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 搜索关键词（小写归一） */
    private String keyword;

    /** 搜索用户 ID，未登录为 NULL */
    private Long userId;

    /** 搜索时间 */
    private LocalDateTime createdAt;
}
