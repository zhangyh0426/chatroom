package com.chatroom.tieba.vo;
import com.chatroom.tieba.entity.ForumPost;
import java.util.List;

public class PostVO extends ForumPost {
    private String authorName;
    private String authorAvatar;
    private List<ReplyVO> replies;
    private String threadTitle; // v1.1: 我的足迹中显示帖子标题

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public List<ReplyVO> getReplies() { return replies; }
    public void setReplies(List<ReplyVO> replies) { this.replies = replies; }
    public String getThreadTitle() { return threadTitle; }
    public void setThreadTitle(String threadTitle) { this.threadTitle = threadTitle; }
}