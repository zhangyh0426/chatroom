package com.chatroom.tieba.dto;

public class UserSessionDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;

    public UserSessionDTO() {}

    public UserSessionDTO(Long id, String username, String nickname, String avatar) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}