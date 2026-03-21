package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.vo.ChatRoomVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatRoomMapper {
    List<ChatRoomVO> findVisibleRoomsByUserId(@Param("userId") Long userId);
    List<ChatRoomVO> findVisibleLegacyRooms();
    int insert(ForumChatRoom room);
    int insertLegacy(ForumChatRoom room);
    int updateCanonicalById(ForumChatRoom room);
    int updateCanonicalByIdLegacy(ForumChatRoom room);
    ForumChatRoom findByRoomCode(@Param("roomCode") String roomCode);
    ForumChatRoom findByRoomCodeLegacy(@Param("roomCode") String roomCode);
    ForumChatRoom findByRoomName(@Param("roomName") String roomName);
    ForumChatRoom findByRoomNameLegacy(@Param("roomName") String roomName);
    ForumChatRoom findById(@Param("id") Long id);
    ForumChatRoom findByIdLegacy(@Param("id") Long id);
    int countMembers(@Param("roomId") Long roomId);
}
