package com.chareslm.shopping.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chareslm.shopping.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper。
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
