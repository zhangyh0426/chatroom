package com.chatroom.tieba.vo;
import com.chatroom.tieba.entity.ForumReply;

public class ReplyVO extends ForumReply {
    private String authorName;
    private String authorAvatar;
    private String replyToUserName;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getReplyToUserName() { return replyToUserName; }
    public void setReplyToUserName(String replyToUserName) { this.replyToUserName = replyToUserName; }
}