package com.chatroom.tieba.service;

import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.chatroom.tieba.vo.ChatRoomVO;

import java.util.List;

public interface ChatRoomService {
    void ensureDefaultGlobalRoomReady();
    List<ChatRoomVO> getRoomList(Long userId);
    ForumChatRoom createRoom(String partitionCode, String roomName);
    void createRoom(String partitionCode, String roomCode, String roomName);
    ForumChatRoom getRoomByCode(String roomCode);
    boolean hasJoined(Long roomId, Long userId);
    boolean joinRoom(Long roomId, Long userId);
    List<ChatMessageVO> getRecentMessages(String roomCode, int limit);
}
