package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumChatMember;
import org.apache.ibatis.annotations.Param;

public interface ChatMemberMapper {
    int insertIgnoreOrInsert(ForumChatMember member);
    ForumChatMember findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);
    int countByRoomId(@Param("roomId") Long roomId);
}
