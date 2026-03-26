SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `tieba_local`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `tieba_local`;

DROP TABLE IF EXISTS `forum_chat_ban`;
DROP TABLE IF EXISTS `forum_chat_message`;
DROP TABLE IF EXISTS `forum_chat_room`;
DROP TABLE IF EXISTS `forum_notification`;
DROP TABLE IF EXISTS `forum_thread_tag`;
DROP TABLE IF EXISTS `forum_tag`;
DROP TABLE IF EXISTS `forum_moderation_log`;
DROP TABLE IF EXISTS `forum_announcement`;
DROP TABLE IF EXISTS `forum_reply`;
DROP TABLE IF EXISTS `forum_post`;
DROP TABLE IF EXISTS `forum_thread_image`;
DROP TABLE IF EXISTS `forum_thread`;
DROP TABLE IF EXISTS `forum_board`;
DROP TABLE IF EXISTS `forum_category`;
DROP TABLE IF EXISTS `forum_user_ban_log`;
DROP TABLE IF EXISTS `forum_user_role`;
DROP TABLE IF EXISTS `forum_role`;
DROP TABLE IF EXISTS `forum_user_profile`;
DROP TABLE IF EXISTS `forum_user_account`;

CREATE TABLE `forum_user_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_user_account_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_profile` (
  `user_id` bigint unsigned NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `avatar_path` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `last_login_ip` varchar(45) DEFAULT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_forum_user_profile_nickname` (`nickname`),
  CONSTRAINT `fk_forum_user_profile_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_code` varchar(30) NOT NULL,
  `role_name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_role_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_forum_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_forum_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`),
  CONSTRAINT `fk_forum_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `forum_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_ban_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `ban_scope` varchar(20) NOT NULL DEFAULT 'FORUM',
  `reason` varchar(255) NOT NULL,
  `start_at` datetime NOT NULL,
  `end_at` datetime DEFAULT NULL,
  `operator_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_user_ban_log_user_id` (`user_id`),
  KEY `idx_forum_user_ban_log_operator_id` (`operator_id`),
  CONSTRAINT `fk_forum_user_ban_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`),
  CONSTRAINT `fk_forum_user_ban_log_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_board` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `category_id` bigint unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `icon_path` varchar(255) DEFAULT NULL,
  `thread_count` int NOT NULL DEFAULT 0,
  `post_count` int NOT NULL DEFAULT 0,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_board_name` (`name`),
  KEY `idx_forum_board_category_id` (`category_id`),
  CONSTRAINT `fk_forum_board_category_id` FOREIGN KEY (`category_id`) REFERENCES `forum_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_thread` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `board_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `thread_type` varchar(20) NOT NULL DEFAULT 'DISCUSSION',
  `cover_image_path` varchar(255) DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT 0,
  `reply_count` int NOT NULL DEFAULT 0,
  `like_count` int NOT NULL DEFAULT 0,
  `is_top` tinyint(1) NOT NULL DEFAULT 0,
  `is_essence` tinyint(1) NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:正常,0:删除',
  `last_reply_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_thread_board_id` (`board_id`),
  KEY `idx_forum_thread_user_id` (`user_id`),
  KEY `idx_forum_thread_last_reply_time` (`last_reply_time`),
  CONSTRAINT `fk_forum_thread_board_id` FOREIGN KEY (`board_id`) REFERENCES `forum_board` (`id`),
  CONSTRAINT `fk_forum_thread_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_thread_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `thread_id` bigint unsigned NOT NULL,
  `sort_no` int NOT NULL DEFAULT 1,
  `file_path` varchar(255) NOT NULL,
  `original_name` varchar(255) DEFAULT NULL,
  `content_type` varchar(100) DEFAULT NULL,
  `file_size` bigint DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:正常,0:删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_thread_image_thread_status_sort` (`thread_id`, `status`, `sort_no`),
  CONSTRAINT `fk_forum_thread_image_thread_id` FOREIGN KEY (`thread_id`) REFERENCES `forum_thread` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `normalized_name` varchar(30) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:正常,0:停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_tag_name` (`name`),
  UNIQUE KEY `uk_forum_tag_normalized_name` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_thread_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `thread_id` bigint unsigned NOT NULL,
  `tag_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_thread_tag_thread_tag` (`thread_id`, `tag_id`),
  KEY `idx_forum_thread_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_forum_thread_tag_thread_id` FOREIGN KEY (`thread_id`) REFERENCES `forum_thread` (`id`),
  CONSTRAINT `fk_forum_thread_tag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `forum_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `thread_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `floor_no` int NOT NULL,
  `content` text NOT NULL,
  `like_count` int NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:正常,0:删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_post_thread_floor` (`thread_id`, `floor_no`),
  KEY `idx_forum_post_user_id` (`user_id`),
  CONSTRAINT `fk_forum_post_thread_id` FOREIGN KEY (`thread_id`) REFERENCES `forum_thread` (`id`),
  CONSTRAINT `fk_forum_post_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `actor_user_id` bigint unsigned DEFAULT NULL,
  `notification_type` varchar(20) NOT NULL,
  `title` varchar(120) NOT NULL,
  `content` varchar(255) DEFAULT NULL,
  `target_type` varchar(20) DEFAULT NULL,
  `target_id` bigint unsigned DEFAULT NULL,
  `target_url` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0:未读,1:已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_notification_user_read_created` (`user_id`, `is_read`, `created_at`),
  KEY `idx_forum_notification_actor_user_id` (`actor_user_id`),
  CONSTRAINT `fk_forum_notification_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`),
  CONSTRAINT `fk_forum_notification_actor_user_id` FOREIGN KEY (`actor_user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_reply` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `thread_id` bigint unsigned NOT NULL,
  `post_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `reply_to_user_id` bigint unsigned DEFAULT NULL,
  `content` text NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:正常,0:删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_reply_thread_id` (`thread_id`),
  KEY `idx_forum_reply_post_id` (`post_id`),
  KEY `idx_forum_reply_user_id` (`user_id`),
  KEY `idx_forum_reply_reply_to_user_id` (`reply_to_user_id`),
  CONSTRAINT `fk_forum_reply_thread_id` FOREIGN KEY (`thread_id`) REFERENCES `forum_thread` (`id`),
  CONSTRAINT `fk_forum_reply_post_id` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`),
  CONSTRAINT `fk_forum_reply_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`),
  CONSTRAINT `fk_forum_reply_reply_to_user_id` FOREIGN KEY (`reply_to_user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_announcement` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PUBLISHED',
  `publish_time` datetime DEFAULT NULL,
  `creator_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_announcement_creator_id` (`creator_id`),
  CONSTRAINT `fk_forum_announcement_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_moderation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `target_type` varchar(30) NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `action_type` varchar(30) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `operator_id` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_moderation_log_target` (`target_type`, `target_id`),
  KEY `idx_forum_moderation_log_operator_id` (`operator_id`),
  CONSTRAINT `fk_forum_moderation_log_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_interest_partition` (
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

CREATE TABLE `forum_chat_room` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `partition_id` bigint unsigned NOT NULL,
  `room_code` varchar(30) NOT NULL,
  `room_name` varchar(100) NOT NULL,
  `room_type` varchar(20) NOT NULL DEFAULT 'PUBLIC',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_chat_room_room_code` (`room_code`),
  KEY `idx_forum_chat_room_partition_id` (`partition_id`),
  CONSTRAINT `fk_forum_chat_room_partition_id` FOREIGN KEY (`partition_id`) REFERENCES `forum_interest_partition` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `message_type` varchar(20) NOT NULL DEFAULT 'TEXT',
  `content` text NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1:可见,0:隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_chat_message_room_id_created_at` (`room_id`, `created_at`),
  KEY `idx_forum_chat_message_user_id` (`user_id`),
  CONSTRAINT `fk_forum_chat_message_room_id` FOREIGN KEY (`room_id`) REFERENCES `forum_chat_room` (`id`),
  CONSTRAINT `fk_forum_chat_message_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_chat_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forum_chat_member_room_user` (`room_id`, `user_id`),
  KEY `idx_forum_chat_member_user_id` (`user_id`),
  CONSTRAINT `fk_forum_chat_member_room_id` FOREIGN KEY (`room_id`) REFERENCES `forum_chat_room` (`id`),
  CONSTRAINT `fk_forum_chat_member_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_chat_ban` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `room_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `reason` varchar(255) NOT NULL,
  `start_at` datetime NOT NULL,
  `end_at` datetime DEFAULT NULL,
  `operator_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_forum_chat_ban_room_id` (`room_id`),
  KEY `idx_forum_chat_ban_user_id` (`user_id`),
  KEY `idx_forum_chat_ban_operator_id` (`operator_id`),
  CONSTRAINT `fk_forum_chat_ban_room_id` FOREIGN KEY (`room_id`) REFERENCES `forum_chat_room` (`id`),
  CONSTRAINT `fk_forum_chat_ban_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`),
  CONSTRAINT `fk_forum_chat_ban_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `forum_role` (`role_code`, `role_name`) VALUES
('USER', '普通用户'),
('MODERATOR', '版主'),
('ADMIN', '管理员');

INSERT INTO `forum_interest_partition` (`partition_code`, `partition_name`, `sort_order`, `status`) VALUES
('SQUARE', '广场大厅', 10, 'ENABLED'),
('TECH_FUN', '技术与娱乐', 20, 'ENABLED'),
('CAMPUS_LIFE', '校园生活', 30, 'ENABLED')
ON DUPLICATE KEY UPDATE
`partition_name` = VALUES(`partition_name`),
`sort_order` = VALUES(`sort_order`),
`status` = VALUES(`status`);

INSERT INTO `forum_chat_room` (`partition_id`, `room_code`, `room_name`, `room_type`, `status`) VALUES
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'SQUARE'), 'GLOBAL', '全站大厅', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'TECH', '技术交流', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'GAME', '游戏讨论', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'MOVIE', '影视漫谈', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'CAMPUS_LIFE'), 'CAMPUS', '校园生活', 'PUBLIC', 'ENABLED')
ON DUPLICATE KEY UPDATE
`partition_id` = VALUES(`partition_id`),
`room_name` = VALUES(`room_name`),
`room_type` = VALUES(`room_type`),
`status` = VALUES(`status`);

SET FOREIGN_KEY_CHECKS = 1;
