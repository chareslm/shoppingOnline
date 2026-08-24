package com.chareslm.shopping.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客服会话 Mapper。
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
