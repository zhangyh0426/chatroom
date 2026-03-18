package com.chatroom.tieba.entity;

import java.util.Date;

public class UserProfile {
    private Long userId;
    private String nickname;
    private String avatarPath;
    private String bio;
    private String lastLoginIp;
    private Date lastLoginAt;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}