package com.chatroom.tieba.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarSchemaStartupCheckerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AvatarSchemaStartupChecker checker;

    @BeforeEach
    void setUp() {
        checker = new AvatarSchemaStartupChecker();
        setField(checker, "jdbcTemplate", jdbcTemplate);
    }

    @Test
    void shouldStayPrimaryModeWhenPrimaryColumnExists() {
        mockColumnCount("avatar_path", 1);
        mockColumnCount("avatar", 1);
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertFalse(checker.shouldPreferFallbackColumn());
        assertTrue(checker.isSchemaReady());
        assertEquals("avatar_path", checker.activeAvatarColumn());
        assertTrue(checker.isInterestPartitionTableReady());
        assertFalse(checker.shouldPreferLegacyChatRoomSchema());
        assertTrue(checker.isChatMemberTableReady());
    }

    @Test
    void shouldEnterFallbackModeWhenPrimaryMissingButFallbackExists() {
        mockColumnCount("avatar_path", 0);
        mockColumnCount("avatar", 1);
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertTrue(checker.shouldPreferFallbackColumn());
        assertTrue(checker.isSchemaReady());
        assertEquals("avatar", checker.activeAvatarColumn());
        assertTrue(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldMarkSchemaNotReadyWhenBothColumnsMissing() {
        mockColumnCount("avatar_path", 0);
        mockColumnCount("avatar", 0);
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertFalse(checker.shouldPreferFallbackColumn());
        assertFalse(checker.isSchemaReady());
        assertEquals("unknown", checker.activeAvatarColumn());
        assertTrue(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldMarkSchemaUnknownWhenSchemaQueryFails() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("forum_user_profile"), eq("avatar_path")))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertFalse(checker.shouldPreferFallbackColumn());
        assertFalse(checker.isSchemaReady());
        assertEquals("unknown", checker.activeAvatarColumn());
        assertTrue(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldMarkInterestPartitionNotReadyWhenTableMissing() {
        mockColumnCount("avatar_path", 1);
        mockColumnCount("avatar", 1);
        mockTableCount("forum_interest_partition", 0);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertTrue(checker.isSchemaReady());
        assertFalse(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldMarkInterestPartitionNotReadyWhenTableQueryFails() {
        mockColumnCount("avatar_path", 1);
        mockColumnCount("avatar", 1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("forum_interest_partition")))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 1);
        mockChatRoomColumnCount("room_name", 1);
        mockChatRoomColumnCount("room_type", 1);
        mockChatRoomColumnCount("name", 0);

        checker.afterPropertiesSet();

        assertTrue(checker.isSchemaReady());
        assertFalse(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldMarkChatInfrastructureNotReadyWhenChatRoomTableMissing() {
        mockColumnCount("avatar_path", 1);
        mockColumnCount("avatar", 1);
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 0);
        mockTableCount("forum_chat_member", 1);

        checker.afterPropertiesSet();

        assertTrue(checker.isSchemaReady());
        assertFalse(checker.isInterestPartitionTableReady());
    }

    @Test
    void shouldEnableLegacyChatRoomCompatibilityWhenOnlyLegacyNameColumnExists() {
        mockColumnCount("avatar_path", 1);
        mockColumnCount("avatar", 1);
        mockTableCount("forum_interest_partition", 1);
        mockTableCount("forum_chat_room", 1);
        mockTableCount("forum_chat_member", 0);
        mockChatRoomColumnCount("room_name", 0);
        mockChatRoomColumnCount("room_type", 0);
        mockChatRoomColumnCount("name", 1);

        checker.afterPropertiesSet();

        assertTrue(checker.isInterestPartitionTableReady());
        assertTrue(checker.shouldPreferLegacyChatRoomSchema());
        assertFalse(checker.isChatMemberTableReady());
    }

    private void mockColumnCount(String columnName, int count) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("forum_user_profile"), eq(columnName)))
                .thenReturn(count);
    }

    private void mockTableCount(String tableName, int count) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(tableName)))
                .thenReturn(count);
    }

    private void mockChatRoomColumnCount(String columnName, int count) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("forum_chat_room"), eq(columnName)))
                .thenReturn(count);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = AvatarSchemaStartupChecker.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
