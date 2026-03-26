package com.chatroom.tieba.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForumPostingBootstrapInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ForumPostingBootstrapInitializer.class);
    private static final String SCHEMA_TABLE_QUERY = """
            SELECT COUNT(1)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    private static final String SCHEMA_COLUMN_QUERY = """
            SELECT COUNT(1)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;
    private static final String ADD_THREAD_TYPE_COLUMN = """
            ALTER TABLE forum_thread
            ADD COLUMN thread_type VARCHAR(20) NOT NULL DEFAULT 'DISCUSSION' AFTER content
            """;
    private static final String ADD_THREAD_COVER_IMAGE_PATH_COLUMN = """
            ALTER TABLE forum_thread
            ADD COLUMN cover_image_path VARCHAR(255) DEFAULT NULL AFTER thread_type
            """;
    private static final String ADD_THREAD_LIKE_COUNT_COLUMN = """
            ALTER TABLE forum_thread
            ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER reply_count
            """;
    private static final String ADD_POST_LIKE_COUNT_COLUMN = """
            ALTER TABLE forum_post
            ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER content
            """;
    private static final String CREATE_THREAD_IMAGE_TABLE = """
            CREATE TABLE forum_thread_image (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              thread_id BIGINT NOT NULL,
              sort_no INT NOT NULL DEFAULT 1,
              file_path VARCHAR(255) NOT NULL,
              original_name VARCHAR(255) DEFAULT NULL,
              content_type VARCHAR(100) DEFAULT NULL,
              file_size BIGINT DEFAULT 0,
              status TINYINT DEFAULT 1 COMMENT '1:正常, 0:删除',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_forum_thread_image_thread_status_sort (thread_id, status, sort_no)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    private static final String CREATE_TAG_TABLE = """
            CREATE TABLE forum_tag (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              name VARCHAR(30) NOT NULL,
              normalized_name VARCHAR(30) NOT NULL,
              status TINYINT DEFAULT 1 COMMENT '1:正常, 0:停用',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              UNIQUE KEY uk_forum_tag_name (name),
              UNIQUE KEY uk_forum_tag_normalized_name (normalized_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    private static final String CREATE_THREAD_TAG_TABLE = """
            CREATE TABLE forum_thread_tag (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              thread_id BIGINT NOT NULL,
              tag_id BIGINT NOT NULL,
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              UNIQUE KEY uk_forum_thread_tag_thread_tag (thread_id, tag_id),
              KEY idx_forum_thread_tag_tag_id (tag_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    private static final String CREATE_NOTIFICATION_TABLE = """
            CREATE TABLE forum_notification (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              user_id BIGINT NOT NULL,
              actor_user_id BIGINT DEFAULT NULL,
              notification_type VARCHAR(20) NOT NULL,
              title VARCHAR(120) NOT NULL,
              content VARCHAR(255) DEFAULT NULL,
              target_type VARCHAR(20) DEFAULT NULL,
              target_id BIGINT DEFAULT NULL,
              target_url VARCHAR(255) DEFAULT NULL,
              is_read TINYINT DEFAULT 0 COMMENT '0:未读, 1:已读',
              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
              KEY idx_forum_notification_user_read_created (user_id, is_read, created_at),
              KEY idx_forum_notification_actor_user_id (actor_user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    private static final String CATEGORY_UPSERT = """
            INSERT INTO forum_category(name, sort_order)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
            sort_order = VALUES(sort_order)
            """;
    private static final String BOARD_UPSERT = """
            INSERT INTO forum_board(category_id, name, description, icon, thread_count, post_count, status, created_at)
            VALUES (
                (SELECT id FROM forum_category WHERE name = ?),
                ?, ?, ?, 0, 0, 1, NOW()
            )
            ON DUPLICATE KEY UPDATE
            category_id = VALUES(category_id),
            description = VALUES(description),
            icon = VALUES(icon),
            status = VALUES(status)
            """;
    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("技术交流", 10),
            new CategorySeed("校园生活", 20),
            new CategorySeed("兴趣讨论", 30)
    );
    private static final List<BoardSeed> BOARD_SEEDS = List.of(
            new BoardSeed("技术交流", "Java开发", "记录日常开发问题、项目实践与踩坑总结。", "J"),
            new BoardSeed("技术交流", "Spring实战", "围绕 Spring / Spring MVC / MyBatis 的项目经验交流。", "S"),
            new BoardSeed("校园生活", "校园杂谈", "聊聊课程、活动、社团和校园日常。", "校"),
            new BoardSeed("校园生活", "二手互助", "闲置转让、拼车拼单和互助信息都可以发在这里。", "换"),
            new BoardSeed("兴趣讨论", "游戏讨论", "游戏资讯、开黑招募和体验分享集中讨论。", "游")
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        reconcilePostingSchema();
        seedDefaultBoards();
    }

    void reconcilePostingSchema() {
        ensureColumnIfMissing("forum_thread", "thread_type", ADD_THREAD_TYPE_COLUMN);
        ensureColumnIfMissing("forum_thread", "cover_image_path", ADD_THREAD_COVER_IMAGE_PATH_COLUMN);
        ensureColumnIfMissing("forum_thread", "like_count", ADD_THREAD_LIKE_COUNT_COLUMN);
        ensureColumnIfMissing("forum_post", "like_count", ADD_POST_LIKE_COUNT_COLUMN);

        ensureTableIfMissing("forum_thread", "forum_thread_image", CREATE_THREAD_IMAGE_TABLE);
        ensureTableIfMissing("forum_thread", "forum_tag", CREATE_TAG_TABLE);
        ensureTableIfMissing("forum_thread", "forum_thread_tag", CREATE_THREAD_TAG_TABLE);
        ensureTableIfMissing("forum_user_profile", "forum_notification", CREATE_NOTIFICATION_TABLE);
    }

    void seedDefaultBoards() {
        try {
            if (!hasTable("forum_category") || !hasTable("forum_board")) {
                log.warn("event=forum_posting_seed_skipped reason=missing_table");
                return;
            }
            for (CategorySeed categorySeed : CATEGORY_SEEDS) {
                jdbcTemplate.update(CATEGORY_UPSERT, categorySeed.name(), categorySeed.sortOrder());
            }
            for (BoardSeed boardSeed : BOARD_SEEDS) {
                jdbcTemplate.update(
                        BOARD_UPSERT,
                        boardSeed.categoryName(),
                        boardSeed.boardName(),
                        boardSeed.description(),
                        boardSeed.icon()
                );
            }
            log.info("event=forum_posting_seed_ready categories={} boards={}",
                    CATEGORY_SEEDS.size(), BOARD_SEEDS.size());
        } catch (DataAccessException ex) {
            log.warn("event=forum_posting_seed_failed exceptionType={} message={}",
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private boolean hasTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject(SCHEMA_TABLE_QUERY, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(SCHEMA_COLUMN_QUERY, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private void ensureColumnIfMissing(String tableName, String columnName, String ddl) {
        try {
            if (!hasTable(tableName) || hasColumn(tableName, columnName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("event=forum_posting_schema_column_added table={} column={}", tableName, columnName);
        } catch (DataAccessException ex) {
            log.warn("event=forum_posting_schema_column_failed table={} column={} message={}",
                    tableName, columnName, ex.getMessage(), ex);
        }
    }

    private void ensureTableIfMissing(String prerequisiteTable, String tableName, String ddl) {
        try {
            if ((prerequisiteTable != null && !hasTable(prerequisiteTable)) || hasTable(tableName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("event=forum_posting_schema_table_created table={}", tableName);
        } catch (DataAccessException ex) {
            log.warn("event=forum_posting_schema_table_failed table={} message={}",
                    tableName, ex.getMessage(), ex);
        }
    }

    private record CategorySeed(String name, int sortOrder) {
    }

    private record BoardSeed(String categoryName, String boardName, String description, String icon) {
    }
}
