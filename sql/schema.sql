CREATE DATABASE IF NOT EXISTS tieba DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tieba;

-- ====================
-- 1. 用户域 (User Domain)
-- ====================
CREATE TABLE `forum_user_account` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(100) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1:正常, 0:封禁',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_profile` (
  `user_id` BIGINT PRIMARY KEY,
  `nickname` VARCHAR(50) NOT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `bio` VARCHAR(500) DEFAULT NULL,
  `last_login_ip` VARCHAR(45) DEFAULT NULL,
  `last_login_time` DATETIME DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_role` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `role_name` VARCHAR(50) NOT NULL UNIQUE,
  `description` VARCHAR(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` INT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_user_ban_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `ban_until` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====================
-- 2. 贴吧域 (Forum Domain)
-- ====================
CREATE TABLE `forum_category` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  `sort_order` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_board` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `category_id` INT NOT NULL,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  `description` VARCHAR(255) DEFAULT NULL,
  `icon` VARCHAR(255) DEFAULT NULL,
  `thread_count` INT DEFAULT 0,
  `post_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1:开启, 0:隐藏/关闭',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_thread` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `board_id` INT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `view_count` INT DEFAULT 0,
  `reply_count` INT DEFAULT 0,
  `is_top` TINYINT DEFAULT 0,
  `is_essence` TINYINT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:删除/隐藏',
  `last_reply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_post` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `thread_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `floor_no` INT NOT NULL,
  `content` TEXT NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_reply` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `thread_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `reply_to_user_id` BIGINT DEFAULT NULL,
  `content` TEXT NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====================
-- 3. 公告与审核域 (Announcement & Moderation Domain)
-- ====================
CREATE TABLE `forum_announcement` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1:发布, 0:草稿/下线',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_moderation_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `operator_id` BIGINT NOT NULL,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'THREAD, POST, REPLY, USER, CHAT',
  `target_id` BIGINT NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====================
-- 4. 聊天室域 (Chat Room Domain)
-- ====================
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

INSERT INTO `forum_interest_partition` (`partition_code`, `partition_name`, `sort_order`, `status`) VALUES
('SQUARE', '广场大厅', 10, 'ENABLED'),
('TECH_FUN', '技术与娱乐', 20, 'ENABLED'),
('CAMPUS_LIFE', '校园生活', 30, 'ENABLED')
ON DUPLICATE KEY UPDATE
`partition_name`=VALUES(`partition_name`),
`sort_order`=VALUES(`sort_order`),
`status`=VALUES(`status`);

INSERT INTO `forum_chat_room` (`partition_id`, `room_code`, `room_name`, `room_type`, `status`) VALUES
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'SQUARE'), 'GLOBAL', '全站大厅', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'TECH', '技术交流', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'GAME', '游戏讨论', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'TECH_FUN'), 'MOVIE', '影视漫谈', 'PUBLIC', 'ENABLED'),
((SELECT id FROM `forum_interest_partition` WHERE `partition_code` = 'CAMPUS_LIFE'), 'CAMPUS', '校园生活', 'PUBLIC', 'ENABLED')
ON DUPLICATE KEY UPDATE
`partition_id`=VALUES(`partition_id`),
`room_name`=VALUES(`room_name`),
`room_type`=VALUES(`room_type`),
`status`=VALUES(`status`);

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

-- Role Init
INSERT INTO `forum_role` (`role_name`, `description`) VALUES 
('ROLE_USER', 'Standard User'), 
('ROLE_ADMIN', 'System Administrator'), 
('ROLE_MODERATOR', 'Forum Moderator')
ON DUPLICATE KEY UPDATE `description`=VALUES(`description`);
