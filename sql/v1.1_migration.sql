-- =============================================
-- v1.1 增量迁移脚本 (在 tieba_local_schema.sql 基础上执行)
-- 功能: 点赞、搜索、个人中心等 v1.1 新功能所需的数据库变更
-- =============================================

USE `tieba_local`;

-- 1. forum_thread 表新增 like_count 字段
ALTER TABLE `forum_thread` ADD COLUMN `like_count` int NOT NULL DEFAULT 0 AFTER `reply_count`;

-- 2. forum_post 表新增 like_count 字段
ALTER TABLE `forum_post` ADD COLUMN `like_count` int NOT NULL DEFAULT 0 AFTER `content`;

-- 3. 新增点赞记录表 (防止重复点赞)
CREATE TABLE IF NOT EXISTS `forum_like_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '点赞用户ID',
  `target_id` bigint unsigned NOT NULL COMMENT '被点赞的内容ID',
  `target_type` varchar(20) NOT NULL COMMENT '内容类型: THREAD, POST',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_user_target` (`user_id`, `target_id`, `target_type`),
  KEY `idx_like_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_like_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';
