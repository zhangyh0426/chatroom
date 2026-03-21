CREATE TABLE IF NOT EXISTS `forum_interest_partition` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `partition_code` varchar(30) NOT NULL,
  `partition_name` varchar(50) NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_interest_partition_code` (`partition_code`),
  UNIQUE KEY `uk_forum_interest_partition_name` (`partition_name`),
  KEY `idx_forum_interest_partition_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `forum_interest_partition` (`partition_code`, `partition_name`, `sort_order`, `status`) VALUES
('SQUARE', '广场大厅', 10, 'ENABLED'),
('TECH_FUN', '技术与娱乐', 20, 'ENABLED'),
('CAMPUS_LIFE', '校园生活', 30, 'ENABLED')
ON DUPLICATE KEY UPDATE
`partition_name` = VALUES(`partition_name`),
`sort_order` = VALUES(`sort_order`),
`status` = VALUES(`status`);

SET @has_chat_room := (
  SELECT COUNT(1)
  FROM `information_schema`.`tables`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'forum_chat_room'
);

SET @has_partition_column := (
  SELECT COUNT(1)
  FROM `information_schema`.`columns`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'forum_chat_room'
    AND `column_name` = 'partition_id'
);

SET @sql := IF(
  @has_chat_room > 0 AND @has_partition_column = 0,
  'ALTER TABLE `forum_chat_room` ADD COLUMN `partition_id` bigint unsigned NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_chat_room > 0,
  'UPDATE `forum_chat_room` SET `partition_id` = (SELECT `id` FROM `forum_interest_partition` WHERE `partition_code` = ''SQUARE'') WHERE `room_code` = ''GLOBAL'' AND (`partition_id` IS NULL OR `partition_id` = 0)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_chat_room > 0,
  'UPDATE `forum_chat_room` SET `partition_id` = (SELECT `id` FROM `forum_interest_partition` WHERE `partition_code` = ''TECH_FUN'') WHERE `room_code` IN (''TECH'', ''GAME'', ''MOVIE'') AND (`partition_id` IS NULL OR `partition_id` = 0)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_chat_room > 0,
  'UPDATE `forum_chat_room` SET `partition_id` = (SELECT `id` FROM `forum_interest_partition` WHERE `partition_code` = ''CAMPUS_LIFE'') WHERE `room_code` = ''CAMPUS'' AND (`partition_id` IS NULL OR `partition_id` = 0)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_chat_room > 0,
  'UPDATE `forum_chat_room` SET `partition_id` = (SELECT `id` FROM `forum_interest_partition` WHERE `partition_code` = ''SQUARE'') WHERE `partition_id` IS NULL OR `partition_id` = 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  @has_chat_room > 0,
  'ALTER TABLE `forum_chat_room` MODIFY COLUMN `partition_id` bigint unsigned NOT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_partition_index := (
  SELECT COUNT(1)
  FROM `information_schema`.`statistics`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'forum_chat_room'
    AND `index_name` = 'idx_forum_chat_room_partition_id'
);

SET @sql := IF(
  @has_chat_room > 0 AND @has_partition_index = 0,
  'ALTER TABLE `forum_chat_room` ADD KEY `idx_forum_chat_room_partition_id` (`partition_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_partition_fk := (
  SELECT COUNT(1)
  FROM `information_schema`.`table_constraints`
  WHERE `table_schema` = DATABASE()
    AND `table_name` = 'forum_chat_room'
    AND `constraint_name` = 'fk_forum_chat_room_partition_id'
    AND `constraint_type` = 'FOREIGN KEY'
);

SET @sql := IF(
  @has_chat_room > 0 AND @has_partition_fk = 0,
  'ALTER TABLE `forum_chat_room` ADD CONSTRAINT `fk_forum_chat_room_partition_id` FOREIGN KEY (`partition_id`) REFERENCES `forum_interest_partition` (`id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
