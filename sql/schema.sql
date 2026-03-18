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
CREATE TABLE `forum_chat_room` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `room_code` VARCHAR(50) NOT NULL UNIQUE,
  `name` VARCHAR(100) NOT NULL,
  `status` TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initialize Global Room
INSERT INTO `forum_chat_room` (`room_code`, `name`) VALUES ('GLOBAL', '全站公共聊天室') ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

CREATE TABLE `forum_chat_message` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `message_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT 'TEXT, IMAGE, SYSTEM',
  `content` TEXT NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:撤回/删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `forum_chat_ban` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `room_id` INT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `ban_until` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Role Init
INSERT INTO `forum_role` (`role_name`, `description`) VALUES 
('ROLE_USER', 'Standard User'), 
('ROLE_ADMIN', 'System Administrator'), 
('ROLE_MODERATOR', 'Forum Moderator')
ON DUPLICATE KEY UPDATE `description`=VALUES(`description`);