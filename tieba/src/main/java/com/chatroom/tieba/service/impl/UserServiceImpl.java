package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.UserAccount;
import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.mapper.UserAccountMapper;
import com.chatroom.tieba.mapper.UserProfileMapper;
import com.chatroom.tieba.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLSyntaxErrorException;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String PROFILE_SAVE_ERROR_MESSAGE = "个人资料保存失败，请稍后重试";
    private static final String PROFILE_READ_ERROR_MESSAGE = "获取个人资料失败，请稍后重试";
    private static final String AVATAR_SCHEMA_ERROR_MESSAGE = "头像更新失败，请联系管理员检查数据库结构";

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    @Override
    @Transactional
    public void register(String username, String password, String nickname) {
        UserAccount existingAccount = userAccountMapper.findByUsername(username);
        if (existingAccount != null) {
            throw new RuntimeException("用户名已存在");
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        UserAccount newAccount = new UserAccount();
        newAccount.setUsername(username);
        newAccount.setPasswordHash(hash);
        newAccount.setStatus(1);
        userAccountMapper.insert(newAccount);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(newAccount.getId());
        userProfile.setNickname(nickname);
        insertProfileWithAvatarFallback(userProfile);
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

        UserProfile profile = findProfileByUserIdWithAvatarFallback(account.getId());
        String nickname = (profile != null && profile.getNickname() != null) ? profile.getNickname() : username;
        String avatar = (profile != null) ? profile.getAvatarPath() : null;

        return new UserSessionDTO(account.getId(), account.getUsername(), nickname, avatar);
    }

    @Override
    public UserProfile getProfileByUserId(Long userId) {
        return findProfileByUserIdWithAvatarFallback(userId);
    }

    @Override
    @Transactional
    public UserProfile updateProfile(Long userId, String nickname, String bio, String avatarPath) {
        String normalizedNickname = normalizeRequired(nickname, "昵称不能为空");
        String normalizedBio = normalizeOptional(bio);
        String normalizedAvatarPath = normalizeOptional(avatarPath);

        UserProfile existingProfile = findProfileByUserIdWithAvatarFallback(userId);
        if (existingProfile == null) {
            UserProfile newProfile = new UserProfile();
            newProfile.setUserId(userId);
            newProfile.setNickname(normalizedNickname);
            newProfile.setBio(normalizedBio);
            newProfile.setAvatarPath(normalizedAvatarPath);
            insertProfileWithAvatarFallback(newProfile);
        } else {
            try {
                userProfileMapper.updateProfile(userId, normalizedNickname, normalizedBio);
            } catch (DataAccessException ex) {
                log.error("event=profile_update_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                        userId, "updateProfile", ex.getClass().getSimpleName(), rootMessage(ex), ex);
                throw new RuntimeException(PROFILE_SAVE_ERROR_MESSAGE);
            }
            if (avatarPath != null) {
                updateAvatarWithFallback(userId, normalizedAvatarPath);
            }
        }

        return findProfileByUserIdWithAvatarFallback(userId);
    }

    private UserProfile findProfileByUserIdWithAvatarFallback(Long userId) {
        try {
            return userProfileMapper.findByUserIdByAvatarPath(userId);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path") || isUnknownColumn(ex, "last_login_at")) {
                log.warn("event=profile_query_primary_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                        userId, "findByUserIdByAvatarPath", ex.getClass().getSimpleName(), rootMessage(ex), ex);
                try {
                    return userProfileMapper.findByUserIdByAvatar(userId);
                } catch (DataAccessException fallbackEx) {
                    log.error("event=profile_query_fallback_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                            userId, "findByUserIdByAvatar", fallbackEx.getClass().getSimpleName(), rootMessage(fallbackEx), fallbackEx);
                    throw new RuntimeException(PROFILE_READ_ERROR_MESSAGE);
                }
            }
            log.error("event=profile_query_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                    userId, "findByUserIdByAvatarPath", ex.getClass().getSimpleName(), rootMessage(ex), ex);
            throw new RuntimeException(PROFILE_READ_ERROR_MESSAGE);
        }
    }

    private void insertProfileWithAvatarFallback(UserProfile profile) {
        try {
            userProfileMapper.insertByAvatarPath(profile);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                log.warn("event=profile_insert_primary_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                        profile.getUserId(), "insertByAvatarPath", ex.getClass().getSimpleName(), rootMessage(ex), ex);
                try {
                    userProfileMapper.insertByAvatar(profile);
                    return;
                } catch (DataAccessException fallbackEx) {
                    log.error("event=profile_insert_fallback_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                            profile.getUserId(), "insertByAvatar", fallbackEx.getClass().getSimpleName(), rootMessage(fallbackEx), fallbackEx);
                }
            } else {
                log.error("event=profile_insert_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                        profile.getUserId(), "insertByAvatarPath", ex.getClass().getSimpleName(), rootMessage(ex), ex);
            }
            throw new RuntimeException(PROFILE_SAVE_ERROR_MESSAGE);
        }
    }

    private void updateAvatarWithFallback(Long userId, String avatarPath) {
        if (avatarSchemaStartupChecker.shouldPreferFallbackColumn()) {
            log.warn("event=avatar_update_compatibility_mode userId={} activeColumn={} schemaReady={}",
                    userId, avatarSchemaStartupChecker.activeAvatarColumn(), avatarSchemaStartupChecker.isSchemaReady());
            updateAvatarByFallbackColumn(userId, avatarPath);
            return;
        }
        try {
            userProfileMapper.updateAvatarByAvatarPath(userId, avatarPath);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                log.warn("event=avatar_update_primary_failed userId={} mapperMethod={} fallbackMapperMethod={} exceptionType={} rootMessage={}",
                        userId, "updateAvatarByAvatarPath", "updateAvatarByAvatar", ex.getClass().getSimpleName(), rootMessage(ex), ex);
                updateAvatarByFallbackColumn(userId, avatarPath);
                return;
            }
            log.error("event=avatar_update_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                    userId, "updateAvatarByAvatarPath", ex.getClass().getSimpleName(), rootMessage(ex), ex);
            if (isSqlSyntaxError(ex)) {
                throw new RuntimeException(AVATAR_SCHEMA_ERROR_MESSAGE);
            }
            throw new RuntimeException(PROFILE_SAVE_ERROR_MESSAGE);
        }
    }

    private void updateAvatarByFallbackColumn(Long userId, String avatarPath) {
        try {
            userProfileMapper.updateAvatarByAvatar(userId, avatarPath);
        } catch (DataAccessException fallbackEx) {
            log.error("event=avatar_update_fallback_failed userId={} mapperMethod={} exceptionType={} rootMessage={}",
                    userId, "updateAvatarByAvatar", fallbackEx.getClass().getSimpleName(), rootMessage(fallbackEx), fallbackEx);
            if (isSqlSyntaxError(fallbackEx)) {
                throw new RuntimeException(AVATAR_SCHEMA_ERROR_MESSAGE);
            }
            throw new RuntimeException(PROFILE_SAVE_ERROR_MESSAGE);
        }
    }

    private boolean isSqlSyntaxError(Throwable throwable) {
        return containsCause(throwable, BadSqlGrammarException.class)
                || containsCause(throwable, SQLSyntaxErrorException.class)
                || containsIgnoreCase(rootMessage(throwable), "unknown column")
                || containsIgnoreCase(rootMessage(throwable), "sql syntax");
    }

    private boolean isUnknownColumn(Throwable throwable, String columnName) {
        return containsIgnoreCase(rootMessage(throwable), "unknown column")
                && containsIgnoreCase(rootMessage(throwable), columnName);
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (type.isInstance(cursor)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor != null ? cursor.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message;
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) {
            return false;
        }
        return source.toLowerCase().contains(target.toLowerCase());
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
