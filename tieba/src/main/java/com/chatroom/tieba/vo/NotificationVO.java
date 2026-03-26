package com.chatroom.tieba.vo;

import com.chatroom.tieba.entity.ForumNotification;

public class NotificationVO extends ForumNotification {
    private String actorNickname;
    private String actorAvatar;

    public String getActorNickname() {
        return actorNickname;
    }

    public void setActorNickname(String actorNickname) {
        this.actorNickname = actorNickname;
    }

    public String getActorAvatar() {
        return actorAvatar;
    }

    public void setActorAvatar(String actorAvatar) {
        this.actorAvatar = actorAvatar;
    }
}
