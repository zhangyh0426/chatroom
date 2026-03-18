# 本地贴吧系统技术文档

## 1. 文档目标

本文档用于定义一个可在本地单机环境运行的贴吧系统技术方案。设计基于现有旧库导出文件 [guestbook.sql](/F:/zhao/chatroom/guestbook.sql)，要求如下：

- 使用 Tomcat 作为运行容器
- 项目全部在本地运行
- 系统必须包含公共聊天室功能
- 旧表和旧字段不修改，只允许新增表和新工程代码

## 2. 当前环境与输入事实

### 2.1 当前仓库现状

当前仓库只有一个数据库导出文件：

- [guestbook.sql](/F:/zhao/chatroom/guestbook.sql)

该 SQL 文件包含以下旧表：

- `category`
- `chart`
- `guestbook`
- `news`
- `userlist`

### 2.2 本机运行环境

已确认的本地环境：

- JDK：`21.0.10`
- Tomcat：`9.0.115`
- Maven：未安装
- Gradle：未安装
- MySQL：当前未检测到安装

结论：

- 新项目应采用 `WAR` 包部署到现有 `Tomcat 9`
- 项目应自带 `Maven Wrapper`
- 本地运行前需要补装 MySQL

## 3. 旧库深度分析

### 3.1 表级问题

#### `userlist`

问题：

- 将账号、资料、管理员状态、登录轨迹、聊天室节流字段混在一张表
- 密码字段长度固定为 32，明显偏向旧式摘要存储
- 缺少清晰的角色模型和封禁模型

判断：

- 不适合作为贴吧系统正式用户模型
- 仅保留历史参考价值

#### `guestbook`

问题：

- 只能表达单一留言流
- 无法区分主题帖、楼层回复、楼中楼回复
- 缺少标题、板块、状态、审核信息

判断：

- 不能支撑贴吧核心内容模型

#### `chart`

问题：

- 表意应为聊天消息，但表名不规范
- 只有 `uid`、`content`、`addtime`
- 无房间、无消息状态、无禁言、无撤回、无审核
- 使用 `MyISAM`，不适合事务性场景

判断：

- 只能作为旧聊天室痕迹，不适合直接扩展

#### `category`

问题：

- 结构过于简单，只能表达最基础分类
- 无法区分“分区”和“吧”

判断：

- 可作为论坛分区设计的历史参考

#### `news`

问题：

- 只有栏目、标题、内容、时间
- 缺少作者、上下线状态、排序能力

判断：

- 仅适合旧公告参考，不适合作为正式运营模型

### 3.2 规范化结论

现有库主要问题不是“字段少”，而是“职责混杂”。从规范化角度看：

- `userlist` 未达到稳定 3NF
- `guestbook` 和 `chart` 基本停留在 1NF 可存储阶段
- 全库缺少事务统一性、外键约束和一致命名

最终结论：

- 旧库保留
- 新贴吧系统采用新增 `forum_*` 表重建主业务模型

## 4. 目标系统边界

本地贴吧系统 v1 范围定义如下：

- 用户注册、登录、退出
- 分区管理
- 吧管理
- 发帖
- 回帖
- 楼中楼回复
- 公告发布
- 后台管理
- 全站公共聊天室

v1 不纳入：

- 私聊
- 群聊
- 搜索引擎
- 积分体系
- 推荐算法
- 移动端 App

## 5. 技术架构

### 5.1 总体架构

单机本地运行拓扑如下：

```text
浏览器
  -> Tomcat 9
     -> Spring MVC
     -> MyBatis
     -> MySQL（本地）
     -> WebSocket 公共聊天室
     -> 本地文件目录（头像、吧图）
```

### 5.2 技术选型

- Java：`JDK 21`
- Web 容器：`Tomcat 9`
- Web 框架：`Spring MVC 5`
- 持久层：`MyBatis`
- 视图层：`JSP + JSTL`
- 连接池：`HikariCP`
- 聊天实现：`javax.websocket`
- 构建方式：`Maven Wrapper`
- 数据库：`MySQL 8.0`，按 `utf8mb4` 设计

选择原则：

- 不引入前后端分离，降低本地部署复杂度
- 不引入微服务，优先保证单体可运行
- 不依赖额外中间件，聊天室直接基于 WebSocket + MySQL 实现

## 6. 数据模型设计

### 6.1 用户域

#### `forum_user_account`

用途：

- 存放登录账号和认证信息

核心字段：

- `id`
- `username`
- `password_hash`
- `status`
- `created_at`
- `updated_at`

#### `forum_user_profile`

用途：

- 存放昵称、头像、简介等资料信息

核心字段：

- `user_id`
- `nickname`
- `avatar`
- `bio`
- `last_login_ip`
- `last_login_time`

#### `forum_role`

用途：

- 定义角色，如普通用户、版主、管理员

#### `forum_user_role`

用途：

- 建立用户与角色的关联

#### `forum_user_ban_log`

用途：

- 记录封禁和解封行为

### 6.2 贴吧域

#### `forum_category`

用途：

- 论坛分区，例如校园、游戏、技术

#### `forum_board`

用途：

- 每个具体贴吧

核心字段：

- `category_id`
- `name`
- `description`
- `icon`
- `thread_count`
- `post_count`
- `status`

#### `forum_thread`

用途：

- 主题帖主表

核心字段：

- `board_id`
- `user_id`
- `title`
- `content`
- `view_count`
- `reply_count`
- `is_top`
- `is_essence`
- `status`
- `last_reply_time`

#### `forum_post`

用途：

- 楼层回复表

核心字段：

- `thread_id`
- `user_id`
- `floor_no`
- `content`
- `status`
- `created_at`

#### `forum_reply`

用途：

- 楼中楼回复表

核心字段：

- `thread_id`
- `post_id`
- `user_id`
- `reply_to_user_id`
- `content`
- `status`
- `created_at`

### 6.3 公告与审核域

#### `forum_announcement`

用途：

- 管理前台公告

#### `forum_moderation_log`

用途：

- 记录帖子、回复、聊天消息的审核操作

### 6.4 聊天室域

#### `forum_chat_room`

用途：

- 聊天房间定义

v1 规则：

- 只创建一个公共房间
- `room_code = GLOBAL`

#### `forum_chat_message`

用途：

- 聊天消息持久化

核心字段：

- `room_id`
- `user_id`
- `message_type`
- `content`
- `status`
- `created_at`

#### `forum_chat_ban`

用途：

- 聊天禁言控制

## 7. 规范化与可维护性策略

数据库设计遵循以下规则：

- 主业务表达到 3NF
- 用户认证与用户资料分离
- 主题帖、楼层、楼中楼分离
- 公告、审核、封禁、聊天独立建模
- 允许少量受控反范式计数字段

受控冗余字段包括：

- `forum_board.thread_count`
- `forum_board.post_count`
- `forum_thread.reply_count`
- `forum_thread.last_reply_time`

这些字段由服务层统一维护，不允许直接由页面层拼接更新。

## 8. 模块设计

### 8.1 前台模块

- 首页
- 分区列表
- 吧列表
- 帖子详情
- 公告列表
- 个人中心
- 公共聊天室

### 8.2 后台模块

- 用户管理
- 分区管理
- 吧管理
- 公告管理
- 帖子审核
- 聊天消息管理
- 封禁管理

## 9. 核心流程设计

### 9.1 登录流程

- 用户提交账号密码
- 服务端校验 `forum_user_account`
- 登录成功写入 Session
- 聊天室连接时校验登录 Session

### 9.2 发帖流程

- 用户进入某个吧
- 提交主题帖
- 写入 `forum_thread`
- 更新 `forum_board.thread_count`

### 9.3 回帖流程

- 用户对主题帖回复
- 写入 `forum_post`
- 更新 `forum_thread.reply_count`
- 更新 `forum_board.post_count`
- 更新 `forum_thread.last_reply_time`

### 9.4 楼中楼流程

- 用户对某一楼层继续回复
- 写入 `forum_reply`

### 9.5 聊天室流程

- 登录用户进入聊天室页面
- 浏览器连接 `/ws/chat/global`
- 服务端校验权限和禁言状态
- 消息写入 `forum_chat_message`
- 服务器向在线用户广播

## 10. 本地部署设计

### 10.1 部署方式

- 浏览器直接访问本机 Tomcat
- Web 应用以 `WAR` 部署
- MySQL 部署在本机
- 上传文件存储在本地目录

### 10.2 目录建议

```text
F:\zhao\chatroom\
  guestbook.sql
  TECHNICAL_DESIGN.md
  tieba\
  data\
    uploads\
  logs\
  sql\
```

### 10.3 端口建议

- Tomcat：`8080`
- MySQL：`3306`

### 10.4 配置项建议

应外置以下配置：

- `jdbc.url`
- `jdbc.username`
- `jdbc.password`
- `upload.path`
- `chat.rate.limit`
- `chat.history.limit`

## 11. 安全与约束

- 密码统一使用 `BCrypt`
- 所有 SQL 使用参数化查询
- 页面输出做 XSS 过滤
- 聊天发送增加频率限制
- 管理员操作写审核日志
- 聊天和发帖都受封禁规则约束

## 12. 验收标准

以下条件全部满足时，视为本地可运行版本达标：

- MySQL 本地安装完成并可建库
- 新表脚本可以执行成功
- 应用可打成 `WAR` 并部署到 Tomcat
- 用户可完成注册、登录、发帖、回帖、楼中楼
- 管理员可完成吧管理、公告管理、审核管理
- 两个浏览器窗口可同时接入聊天室并实时通信
- 旧表结构保持不变

## 13. 最终技术结论

本项目应采用“旧库保留、主业务重建”的方式实现，不应在原留言板模型上继续打补丁。最合理的本地运行方案是：

- `Tomcat 9 + Spring MVC + MyBatis + JSP`
- `MySQL 本地部署`
- `forum_*` 规范化业务模型
- `WebSocket` 公共聊天室
- `WAR` 单体部署

这份文档作为后续数据库 DDL、项目初始化、接口设计和页面开发的统一基线。
