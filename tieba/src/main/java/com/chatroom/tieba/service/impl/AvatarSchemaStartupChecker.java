package com.chatroom.tieba.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AvatarSchemaStartupChecker implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AvatarSchemaStartupChecker.class);
    private static final String PROFILE_TABLE = "forum_user_profile";
    private static final String INTEREST_PARTITION_TABLE = "forum_interest_partition";
    private static final String CHAT_ROOM_TABLE = "forum_chat_room";
    private static final String CHAT_MEMBER_TABLE = "forum_chat_member";
    private static final String PRIMARY_AVATAR_COLUMN = "avatar_path";
    private static final String FALLBACK_AVATAR_COLUMN = "avatar";
    private static final String PRIMARY_CHAT_ROOM_NAME_COLUMN = "room_name";
    private static final String LEGACY_CHAT_ROOM_NAME_COLUMN = "name";
    private static final String PRIMARY_CHAT_ROOM_TYPE_COLUMN = "room_type";
    private static final String SCHEMA_COLUMN_QUERY = """
            SELECT COUNT(1)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;
    private static final String SCHEMA_TABLE_QUERY = """
            SELECT COUNT(1)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    private static final String INTEREST_PARTITION_MIGRATION_HINT = "SOURCE sql/v1.2_interest_partition_migration.sql;";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile AvatarSchemaMode avatarSchemaMode = AvatarSchemaMode.UNKNOWN;
    private volatile boolean chatInfrastructureReady;
    private volatile boolean chatMemberTableReady;
    private volatile ChatRoomSchemaMode chatRoomSchemaMode = ChatRoomSchemaMode.UNKNOWN;

    @Override
    public void afterPropertiesSet() {
        refreshSchemaMode();
        refreshChatSchemaMode();
    }

    public boolean shouldPreferFallbackColumn() {
        return avatarSchemaMode == AvatarSchemaMode.FALLBACK_ONLY;
    }

    public boolean isSchemaReady() {
        return avatarSchemaMode == AvatarSchemaMode.PRIMARY_READY
                || avatarSchemaMode == AvatarSchemaMode.FALLBACK_ONLY;
    }

    public String activeAvatarColumn() {
        return switch (avatarSchemaMode) {
            case PRIMARY_READY -> PRIMARY_AVATAR_COLUMN;
            case FALLBACK_ONLY -> FALLBACK_AVATAR_COLUMN;
            default -> "unknown";
        };
    }

    public boolean isInterestPartitionTableReady() {
        return chatInfrastructureReady;
    }

    public boolean probeInterestPartitionTableReady() {
        refreshChatSchemaMode();
        return chatInfrastructureReady;
    }

    public boolean shouldPreferLegacyChatRoomSchema() {
        return chatRoomSchemaMode == ChatRoomSchemaMode.LEGACY_COMPAT;
    }

    public boolean isChatMemberTableReady() {
        return chatMemberTableReady;
    }

    void refreshSchemaMode() {
        try {
            boolean hasPrimaryColumn = hasColumn(PRIMARY_AVATAR_COLUMN);
            boolean hasFallbackColumn = hasColumn(FALLBACK_AVATAR_COLUMN);
            if (hasPrimaryColumn) {
                avatarSchemaMode = AvatarSchemaMode.PRIMARY_READY;
                log.info("event=avatar_schema_ready mode={} table={} activeColumn={}",
                        avatarSchemaMode.name(), PROFILE_TABLE, PRIMARY_AVATAR_COLUMN);
                return;
            }
            if (hasFallbackColumn) {
                avatarSchemaMode = AvatarSchemaMode.FALLBACK_ONLY;
                log.warn("event=avatar_schema_alert alertLevel=HIGH mode={} table={} missingColumn={} fallbackColumn={}",
                        avatarSchemaMode.name(), PROFILE_TABLE, PRIMARY_AVATAR_COLUMN, FALLBACK_AVATAR_COLUMN);
                return;
            }
            avatarSchemaMode = AvatarSchemaMode.BROKEN;
            log.error("event=avatar_schema_alert alertLevel=HIGH mode={} table={} missingColumn={} fallbackColumn={} action=manual_fix_required",
                    avatarSchemaMode.name(), PROFILE_TABLE, PRIMARY_AVATAR_COLUMN, FALLBACK_AVATAR_COLUMN);
        } catch (DataAccessException ex) {
            avatarSchemaMode = AvatarSchemaMode.UNKNOWN;
            log.error("event=avatar_schema_check_failed alertLevel=HIGH table={} exceptionType={} message={}",
                    PROFILE_TABLE, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    void refreshChatSchemaMode() {
        try {
            boolean hasPartitionTable = hasTable(INTEREST_PARTITION_TABLE);
            boolean hasRoomTable = hasTable(CHAT_ROOM_TABLE);
            chatRoomSchemaMode = resolveChatRoomSchemaMode(hasRoomTable);
            chatMemberTableReady = hasTable(CHAT_MEMBER_TABLE);
            chatInfrastructureReady = hasPartitionTable
                    && hasRoomTable
                    && chatRoomSchemaMode != ChatRoomSchemaMode.BROKEN;
            if (chatInfrastructureReady) {
                log.info("event=chat_schema_ready tables={},{} roomSchemaMode={} memberTableReady={}",
                        INTEREST_PARTITION_TABLE, CHAT_ROOM_TABLE, chatRoomSchemaMode.name(), chatMemberTableReady);
                if (!chatMemberTableReady) {
                    log.warn("event=chat_member_schema_alert alertLevel=MEDIUM table={} action=compat_mode_enabled impact=room_membership_relaxed",
                            CHAT_MEMBER_TABLE);
                }
                return;
            }
            List<String> missingTables = new ArrayList<>();
            if (!hasPartitionTable) {
                missingTables.add(INTEREST_PARTITION_TABLE);
            }
            if (!hasRoomTable) {
                missingTables.add(CHAT_ROOM_TABLE);
            }
            if (chatRoomSchemaMode == ChatRoomSchemaMode.BROKEN) {
                log.warn("event=chat_schema_alert alertLevel=HIGH table={} roomSchemaMode={} action=run_migration hint={} impact=chat_module_only nonChatStartup=unaffected",
                        CHAT_ROOM_TABLE, chatRoomSchemaMode.name(), INTEREST_PARTITION_MIGRATION_HINT);
                return;
            }
            log.warn("event=chat_schema_alert alertLevel=HIGH tablesMissing={} action=run_migration hint={} impact=chat_module_only nonChatStartup=unaffected",
                    String.join(",", missingTables), INTEREST_PARTITION_MIGRATION_HINT);
        } catch (DataAccessException ex) {
            chatInfrastructureReady = false;
            chatMemberTableReady = false;
            chatRoomSchemaMode = ChatRoomSchemaMode.UNKNOWN;
            log.warn("event=chat_schema_check_failed alertLevel=HIGH table={} exceptionType={} message={} impact=chat_module_only nonChatStartup=unaffected",
                    INTEREST_PARTITION_TABLE, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
    }

    private boolean hasColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject(SCHEMA_COLUMN_QUERY, Integer.class, PROFILE_TABLE, columnName);
        return count != null && count > 0;
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(SCHEMA_COLUMN_QUERY, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean hasTable(String tableName) {
        Integer count = jdbcTemplate.queryForObject(SCHEMA_TABLE_QUERY, Integer.class, tableName);
        return count != null && count > 0;
    }

    private ChatRoomSchemaMode resolveChatRoomSchemaMode(boolean hasRoomTable) {
        if (!hasRoomTable) {
            return ChatRoomSchemaMode.UNKNOWN;
        }
        boolean hasPrimaryName = hasColumn(CHAT_ROOM_TABLE, PRIMARY_CHAT_ROOM_NAME_COLUMN);
        boolean hasRoomType = hasColumn(CHAT_ROOM_TABLE, PRIMARY_CHAT_ROOM_TYPE_COLUMN);
        if (hasPrimaryName && hasRoomType) {
            return ChatRoomSchemaMode.PRIMARY_READY;
        }
        if (hasColumn(CHAT_ROOM_TABLE, LEGACY_CHAT_ROOM_NAME_COLUMN)) {
            return ChatRoomSchemaMode.LEGACY_COMPAT;
        }
        return ChatRoomSchemaMode.BROKEN;
    }

    private enum AvatarSchemaMode {
        PRIMARY_READY,
        FALLBACK_ONLY,
        BROKEN,
        UNKNOWN
    }

    private enum ChatRoomSchemaMode {
        PRIMARY_READY,
        LEGACY_COMPAT,
        BROKEN,
        UNKNOWN
    }
}
