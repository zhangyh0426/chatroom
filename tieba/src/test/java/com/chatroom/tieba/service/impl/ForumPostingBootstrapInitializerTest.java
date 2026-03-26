package com.chatroom.tieba.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForumPostingBootstrapInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ForumPostingBootstrapInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new ForumPostingBootstrapInitializer();
        setField("jdbcTemplate", jdbcTemplate);
    }

    @Test
    void shouldSeedDefaultCategoriesAndBoardsWhenTablesExist() {
        mockTableCount("forum_category", 1);
        mockTableCount("forum_board", 1);

        initializer.afterPropertiesSet();

        verify(jdbcTemplate).update(contains("INSERT INTO forum_category"), eq("技术交流"), eq(10));
        verify(jdbcTemplate).update(contains("INSERT INTO forum_board"),
                eq("技术交流"), eq("Java开发"), eq("记录日常开发问题、项目实践与踩坑总结。"), eq("J"));
    }

    @Test
    void shouldSkipSeedingWhenPostingTablesAreMissing() {
        mockTableCount("forum_category", 0);
        mockTableCount("forum_board", 1);

        initializer.afterPropertiesSet();

        verify(jdbcTemplate, never()).update(contains("INSERT INTO forum_category"), eq("技术交流"), eq(10));
        verify(jdbcTemplate, never()).update(contains("INSERT INTO forum_board"),
                eq("技术交流"), eq("Java开发"), eq("记录日常开发问题、项目实践与踩坑总结。"), eq("J"));
    }

    @Test
    void shouldBackfillLegacyThreadColumnsWhenMissing() {
        mockTableCount("forum_thread", 1);
        mockTableCount("forum_post", 1);
        mockColumnCount("forum_thread", "thread_type", 0);
        mockColumnCount("forum_thread", "cover_image_path", 0);
        mockColumnCount("forum_thread", "like_count", 0);
        mockColumnCount("forum_post", "like_count", 0);

        initializer.afterPropertiesSet();

        verify(jdbcTemplate).execute(contains("ADD COLUMN thread_type"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN cover_image_path"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER reply_count"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER content"));
    }

    @Test
    void shouldCreateDiscoveryAndNotificationTablesForLegacyDatabases() {
        mockTableCount("forum_thread", 1);
        mockTableCount("forum_user_profile", 1);
        mockTableCount("forum_thread_image", 0);
        mockTableCount("forum_tag", 0);
        mockTableCount("forum_thread_tag", 0);
        mockTableCount("forum_notification", 0);

        initializer.afterPropertiesSet();

        verify(jdbcTemplate).execute(contains("CREATE TABLE forum_thread_image"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE forum_tag"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE forum_thread_tag"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE forum_notification"));
    }

    private void mockTableCount(String tableName, int count) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(tableName)))
                .thenReturn(count);
    }

    private void mockColumnCount(String tableName, String columnName, int count) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(tableName), eq(columnName)))
                .thenReturn(count);
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = ForumPostingBootstrapInitializer.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(initializer, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
