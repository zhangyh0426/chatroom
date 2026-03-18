package com.chatroom.tieba.entity;
import java.util.Date;

public class ForumBoard {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private String icon;
    private Integer threadCount;
    private Integer postCount;
    private Integer status;
    private Date createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getThreadCount() { return threadCount; }
    public void setThreadCount(Integer threadCount) { this.threadCount = threadCount; }
    public Integer getPostCount() { return postCount; }
    public void setPostCount(Integer postCount) { this.postCount = postCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}