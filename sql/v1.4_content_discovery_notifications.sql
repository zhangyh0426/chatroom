USE `tieba`;

ALTER TABLE `forum_thread`
  ADD COLUMN IF NOT EXISTS `thread_type` VARCHAR(20) NOT NULL DEFAULT 'DISCUSSION' AFTER `content`,
  ADD COLUMN IF NOT EXISTS `cover_image_path` VARCHAR(255) DEFAULT NULL AFTER `thread_type`,
  ADD COLUMN IF NOT EXISTS `like_count` INT NOT NULL DEFAULT 0 AFTER `reply_count`;

ALTER TABLE `forum_post`
  ADD COLUMN IF NOT EXISTS `like_count` INT NOT NULL DEFAULT 0 AFTER `content`;

CREATE TABLE IF NOT EXISTS `forum_tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(30) NOT NULL,
  `normalized_name` VARCHAR(30) NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_forum_tag_name` (`name`),
  UNIQUE KEY `uk_forum_tag_normalized_name` (`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `forum_thread_tag` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `thread_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_forum_thread_tag_thread_tag` (`thread_id`, `tag_id`),
  KEY `idx_forum_thread_tag_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `forum_notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `actor_user_id` BIGINT DEFAULT NULL,
  `notification_type` VARCHAR(20) NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `content` VARCHAR(255) DEFAULT NULL,
  `target_type` VARCHAR(20) DEFAULT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `target_url` VARCHAR(255) DEFAULT NULL,
  `is_read` TINYINT DEFAULT 0 COMMENT '0:未读, 1:已读',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_forum_notification_user_read_created` (`user_id`, `is_read`, `created_at`),
  KEY `idx_forum_notification_actor_user_id` (`actor_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
