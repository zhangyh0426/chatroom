package com.chatroom.tieba.service;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.UserProfile;

public interface UserService {
    void register(String username, String password, String nickname);
    UserSessionDTO login(String username, String password);
    UserProfile getProfileByUserId(Long userId);
    UserProfile updateProfile(Long userId, String nickname, String bio, String avatarPath);
}
