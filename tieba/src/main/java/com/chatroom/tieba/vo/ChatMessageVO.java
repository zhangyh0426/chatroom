package com.chatroom.tieba.vo;

import com.chatroom.tieba.entity.ForumChatMessage;

public class ChatMessageVO extends ForumChatMessage {
    private String nickname;
    private String avatar;
    private String roomCode;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
}
