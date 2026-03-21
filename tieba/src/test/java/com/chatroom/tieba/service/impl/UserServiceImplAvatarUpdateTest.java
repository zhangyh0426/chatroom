package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.mapper.UserAccountMapper;
import com.chatroom.tieba.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

import java.lang.reflect.Field;
import java.sql.SQLSyntaxErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAvatarUpdateTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        setField(userService, "userProfileMapper", userProfileMapper);
        setField(userService, "userAccountMapper", userAccountMapper);
        setField(userService, "avatarSchemaStartupChecker", avatarSchemaStartupChecker);
        when(avatarSchemaStartupChecker.shouldPreferFallbackColumn()).thenReturn(false);
    }

    @Test
    void shouldUpdateAvatarByPrimaryPathWhenPrimaryColumnAvailable() {
        Long userId = 101L;
        UserProfile profile = buildProfile(userId);
        when(userProfileMapper.findByUserIdByAvatarPath(userId)).thenReturn(profile);

        userService.updateProfile(userId, "nick", "bio", "/a.png");

        verify(userProfileMapper).updateAvatarByAvatarPath(userId, "/a.png");
        verify(userProfileMapper, never()).updateAvatarByAvatar(eq(userId), anyString());
    }

    @Test
    void shouldFallbackToCompatibleColumnWhenPrimaryPathReportsUnknownColumn() {
        Long userId = 102L;
        UserProfile profile = buildProfile(userId);
        when(userProfileMapper.findByUserIdByAvatarPath(userId)).thenReturn(profile);
        when(userProfileMapper.updateAvatarByAvatarPath(userId, "/b.png"))
                .thenThrow(unknownColumn("avatar_path"));

        userService.updateProfile(userId, "nick", "bio", "/b.png");

        verify(userProfileMapper).updateAvatarByAvatarPath(userId, "/b.png");
        verify(userProfileMapper).updateAvatarByAvatar(userId, "/b.png");
    }

    @Test
    void shouldThrowSchemaErrorWhenFallbackPathAlsoFailsBySqlSyntax() {
        Long userId = 103L;
        UserProfile profile = buildProfile(userId);
        when(userProfileMapper.findByUserIdByAvatarPath(userId)).thenReturn(profile);
        when(userProfileMapper.updateAvatarByAvatarPath(userId, "/c.png"))
                .thenThrow(unknownColumn("avatar_path"));
        when(userProfileMapper.updateAvatarByAvatar(userId, "/c.png"))
                .thenThrow(sqlSyntax("You have an error in your SQL syntax"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateProfile(userId, "nick", "bio", "/c.png"));

        assertEquals("头像更新失败，请联系管理员检查数据库结构", exception.getMessage());
    }

    private UserProfile buildProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("tester");
        return profile;
    }

    private BadSqlGrammarException unknownColumn(String columnName) {
        return new BadSqlGrammarException("update avatar", "UPDATE forum_user_profile",
                new SQLSyntaxErrorException("Unknown column '" + columnName + "' in 'field list'"));
    }

    private BadSqlGrammarException sqlSyntax(String message) {
        return new BadSqlGrammarException("update avatar", "UPDATE forum_user_profile",
                new SQLSyntaxErrorException(message));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = UserServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
