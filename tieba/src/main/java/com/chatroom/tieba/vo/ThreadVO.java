package com.chatroom.tieba.vo;
import com.chatroom.tieba.entity.ForumThread;

public class ThreadVO extends ForumThread {
    private String authorName;
    private String authorAvatar;
    private String boardName; // v1.1: 搜索/我的足迹中显示吧名

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }
}