package com.chatroom.tieba.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingSchemaContractTest {

    @Test
    void schemaShouldContainThreadImageTableAndDefaultBoardSeeds() throws Exception {
        String schemaSql = Files.readString(
                Path.of("..", "sql", "schema.sql"),
                StandardCharsets.UTF_8);

        assertTrue(schemaSql.contains("CREATE TABLE `forum_thread_image`"));
        assertTrue(schemaSql.contains("INSERT INTO `forum_category` (`name`, `sort_order`) VALUES"));
        assertTrue(schemaSql.contains("'Java开发'"));
        assertTrue(schemaSql.contains("'Spring实战'"));
        assertTrue(schemaSql.contains("'校园杂谈'"));
        assertTrue(schemaSql.contains("'二手互助'"));
        assertTrue(schemaSql.contains("'游戏讨论'"));
    }

    @Test
    void migrationShouldBootstrapPostingPageForExistingDatabases() throws Exception {
        String migrationSql = Files.readString(
                Path.of("..", "sql", "v1.3_posting_page_bootstrap.sql"),
                StandardCharsets.UTF_8);

        assertTrue(migrationSql.contains("CREATE TABLE IF NOT EXISTS `forum_thread_image`"));
        assertTrue(migrationSql.contains("INSERT INTO `forum_category` (`name`, `sort_order`) VALUES"));
        assertTrue(migrationSql.contains("INSERT INTO `forum_board` (`category_id`, `name`, `description`, `icon`, `thread_count`, `post_count`, `status`) VALUES"));
    }

    @Test
    void migrationShouldContainContentDiscoveryAndNotificationSchema() throws Exception {
        String migrationSql = Files.readString(
                Path.of("..", "sql", "v1.4_content_discovery_notifications.sql"),
                StandardCharsets.UTF_8);

        assertTrue(migrationSql.contains("ADD COLUMN IF NOT EXISTS `thread_type`"));
        assertTrue(migrationSql.contains("ADD COLUMN IF NOT EXISTS `cover_image_path`"));
        assertTrue(migrationSql.contains("CREATE TABLE IF NOT EXISTS `forum_tag`"));
        assertTrue(migrationSql.contains("CREATE TABLE IF NOT EXISTS `forum_thread_tag`"));
        assertTrue(migrationSql.contains("CREATE TABLE IF NOT EXISTS `forum_notification`"));
    }
}
