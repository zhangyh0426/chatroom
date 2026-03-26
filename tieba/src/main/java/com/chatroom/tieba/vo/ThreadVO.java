package com.chatroom.tieba.vo;
import com.chatroom.tieba.entity.ForumThread;
import com.chatroom.tieba.support.ThreadTypeCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadVO extends ForumThread {
    private String authorName;
    private String authorAvatar;
    private String boardName; // v1.1: 搜索/我的足迹中显示吧名
    private String tagNamesJoined;
    private List<String> tagNames = List.of();
    private List<ThreadImageVO> images;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }
    public String getTagNamesJoined() { return tagNamesJoined; }
    public void setTagNamesJoined(String tagNamesJoined) { this.tagNamesJoined = tagNamesJoined; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            this.tagNames = List.of();
            return;
        }
        this.tagNames = Collections.unmodifiableList(new ArrayList<>(tagNames));
    }
    public String getThreadTypeLabel() { return ThreadTypeCatalog.labelOf(getThreadType()); }
    public List<ThreadImageVO> getImages() { return images; }
    public void setImages(List<ThreadImageVO> images) { this.images = images; }
}
