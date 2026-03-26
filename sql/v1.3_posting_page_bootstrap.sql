CREATE TABLE IF NOT EXISTS `forum_thread_image` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `thread_id` BIGINT NOT NULL,
  `sort_no` INT NOT NULL DEFAULT 1,
  `file_path` VARCHAR(255) NOT NULL,
  `original_name` VARCHAR(255) DEFAULT NULL,
  `content_type` VARCHAR(100) DEFAULT NULL,
  `file_size` BIGINT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1:正常, 0:删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_forum_thread_image_thread_status_sort` (`thread_id`, `status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `forum_category` (`name`, `sort_order`) VALUES
('技术交流', 10),
('校园生活', 20),
('兴趣讨论', 30)
ON DUPLICATE KEY UPDATE
`sort_order` = VALUES(`sort_order`);

INSERT INTO `forum_board` (`category_id`, `name`, `description`, `icon`, `thread_count`, `post_count`, `status`) VALUES
((SELECT id FROM `forum_category` WHERE `name` = '技术交流'), 'Java开发', '记录日常开发问题、项目实践与踩坑总结。', 'J', 0, 0, 1),
((SELECT id FROM `forum_category` WHERE `name` = '技术交流'), 'Spring实战', '围绕 Spring / Spring MVC / MyBatis 的项目经验交流。', 'S', 0, 0, 1),
((SELECT id FROM `forum_category` WHERE `name` = '校园生活'), '校园杂谈', '聊聊课程、活动、社团和校园日常。', '校', 0, 0, 1),
((SELECT id FROM `forum_category` WHERE `name` = '校园生活'), '二手互助', '闲置转让、拼车拼单和互助信息都可以发在这里。', '换', 0, 0, 1),
((SELECT id FROM `forum_category` WHERE `name` = '兴趣讨论'), '游戏讨论', '游戏资讯、开黑招募和体验分享集中讨论。', '游', 0, 0, 1)
ON DUPLICATE KEY UPDATE
`category_id` = VALUES(`category_id`),
`description` = VALUES(`description`),
`icon` = VALUES(`icon`),
`status` = VALUES(`status`);
