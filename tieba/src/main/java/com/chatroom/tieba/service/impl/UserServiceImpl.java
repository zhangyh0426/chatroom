package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.UserAccount;
import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.mapper.UserAccountMapper;
import com.chatroom.tieba.mapper.UserProfileMapper;
import com.chatroom.tieba.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    @Transactional
    public void register(String username, String password, String nickname) {
        // 1. 判断用户名是否已存在
        UserAccount existingAccount = userAccountMapper.findByUsername(username);
        if (existingAccount != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 加密密码
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        // 3. 插入用户账号
        UserAccount newAccount = new UserAccount();
        newAccount.setUsername(username);
        newAccount.setPasswordHash(hash);
        newAccount.setStatus(1);
        userAccountMapper.insert(newAccount);

        // 4. 插入用户资料
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(newAccount.getId());
        userProfile.setNickname(nickname);
        userProfileMapper.insert(userProfile);
    }

    @Override
    public UserSessionDTO login(String username, String password) {
        UserAccount account = userAccountMapper.findByUsername(username);
        if (account == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (account.getStatus() == 0) {
            throw new RuntimeException("该账号已被封禁");
        }

        if (!BCrypt.checkpw(password, account.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        UserProfile profile = userProfileMapper.findByUserId(account.getId());
        String nickname = (profile != null && profile.getNickname() != null) ? profile.getNickname() : username;
        String avatar = (profile != null) ? profile.getAvatarPath() : null;

        return new UserSessionDTO(account.getId(), account.getUsername(), nickname, avatar);
    }

    @Override
    public UserProfile getProfileByUserId(Long userId) {
        return userProfileMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserProfile updateProfile(Long userId, String nickname, String bio, String avatarPath) {
        String normalizedNickname = normalizeRequired(nickname, "昵称不能为空");
        String normalizedBio = normalizeOptional(bio);

        UserProfile existingProfile = userProfileMapper.findByUserId(userId);
        if (existingProfile == null) {
            UserProfile newProfile = new UserProfile();
            newProfile.setUserId(userId);
            newProfile.setNickname(normalizedNickname);
            newProfile.setBio(normalizedBio);
            newProfile.setAvatarPath(normalizeOptional(avatarPath));
            userProfileMapper.insert(newProfile);
        } else {
            userProfileMapper.updateProfile(userId, normalizedNickname, normalizedBio);
            if (avatarPath != null) {
                userProfileMapper.updateAvatar(userId, normalizeOptional(avatarPath));
            }
        }

        return userProfileMapper.findByUserId(userId);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
