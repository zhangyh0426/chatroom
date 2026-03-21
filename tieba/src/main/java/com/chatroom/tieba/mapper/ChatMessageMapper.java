package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumChatMessage;
import com.chatroom.tieba.vo.ChatMessageVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ChatMessageMapper {
    int insert(ForumChatMessage msg);
    List<ChatMessageVO> findRecentMessagesByRoomId(@Param("roomId") Long roomId, @Param("limit") int limit);
    List<ChatMessageVO> findRecentMessagesByRoomIdByAvatar(@Param("roomId") Long roomId, @Param("limit") int limit);
}
