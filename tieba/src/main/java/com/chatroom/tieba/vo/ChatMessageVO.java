package com.chatroom.tieba.vo;

import com.chatroom.tieba.entity.ForumChatMessage;

public class ChatMessageVO extends ForumChatMessage {
    private String nickname;
    private String avatar;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}