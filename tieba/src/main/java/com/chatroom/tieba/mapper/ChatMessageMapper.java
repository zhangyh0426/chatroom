package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumChatMessage;
import com.chatroom.tieba.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ChatMessageMapper {
    int insert(ForumChatMessage msg);
    List<ChatMessageVO> findRecentGlobalMessages(@Param("limit") int limit);
}