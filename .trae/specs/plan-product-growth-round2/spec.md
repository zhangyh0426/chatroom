# 第二轮学习型聊天软件功能 Spec

## Why

这是学生作业场景，目标是学习聊天软件核心能力，不需要封禁、审核后台等运营治理功能。\
第二轮聚焦“可讲解、可演示、可验证”的聊天产品功能闭环，覆盖私聊、聊天室体验和消息交互。

## What Changes

* 规划“私聊会话（1v1）”功能，形成从公共聊天到定向沟通的完整链路
* 规划“聊天室连续性（未读、断线补拉、历史条数配置化）”提升会话体验
* 规划“在线用户列表 + 正在输入状态”，强化实时互动感知
* 规划“消息交互增强（@提及、表情快捷、消息状态）”提升可用性
* 规划“全站搜索入口 + 搜索结果页”提升内容发现与学习演示价值
* 产出第二轮优先级、MVP 边界、验收指标（消息发送成功率、会话留存率、搜索点击率）

## Impact

* Affected specs: 私聊会话、聊天室体验、实时状态感知、消息交互、内容发现

* Affected code: `controller/*`、`service/*`、`websocket/*`、`mapper/*`、`WEB-INF/jsp/*`、`sql/tieba_local_schema.sql`

## ADDED Requirements

### Requirement: Message Notification Center

系统 SHALL 提供消息通知中心，支持“私聊新消息”“@提及我”两类通知，并展示未读状态。

#### Scenario: Receive private message notification

* **WHEN** 用户离开会话列表后收到新的私聊消息

* **THEN** 用户在通知中心看到一条未读“私聊新消息”并可跳转到对应会话

#### Scenario: Receive mention notification

* **WHEN** 用户在公共聊天室被他人 @提及

* **THEN** 用户在通知中心看到一条未读“@提及我”并可一键标记已读

### Requirement: Private Chat Session

系统 SHALL 提供 1v1 私聊会话能力，支持发起会话、查看会话列表、收发消息与历史查看。

#### Scenario: Start private chat

* **WHEN** 用户在在线列表中选择另一名用户并发起私聊

* **THEN** 系统创建或复用会话并进入私聊窗口

#### Scenario: Send private message

* **WHEN** 用户在私聊窗口发送文本消息

* **THEN** 对方实时接收并在双方会话中可见

### Requirement: Chat Continuity

系统 SHALL 在公共聊天室提供断线补拉与未读提示，并支持历史条数配置化读取。

#### Scenario: Reconnect and backfill

* **WHEN** 用户网络中断后重新连接聊天室

* **THEN** 系统自动补拉断线期间消息并展示未读数量

### Requirement: Presence Awareness

系统 SHALL 提供在线用户列表与“正在输入”状态，帮助用户判断沟通时机。

#### Scenario: Show online users

* **WHEN** 用户进入公共聊天室

* **THEN** 页面展示当前在线用户列表并实时更新

#### Scenario: Typing indicator

* **WHEN** 某用户在输入框持续输入

* **THEN** 其他用户看到其“正在输入”状态提示

### Requirement: Message Interaction Enhancement

系统 SHALL 支持消息交互增强能力，至少包含 @提及、表情快捷插入、消息发送状态（发送中/成功/失败）。

#### Scenario: Mention user in message

* **WHEN** 用户输入 @ 并选择目标用户发送消息

* **THEN** 消息中高亮提及对象并在对方端可识别

### Requirement: Global Search Entry

系统 SHALL 提供全站搜索入口与结果页，支持按相关度与时间排序切换。

#### Scenario: Search from header

* **WHEN** 用户在全站导航输入关键词

* **THEN** 跳转到搜索结果页并返回匹配帖子

## MODIFIED Requirements

### Requirement: Product Iteration Priority

第二轮功能实施必须遵循“聊天核心链路优先、体验增强其次、增长入口最后”的节奏，先保证可演示的完整聊天闭环。

## REMOVED Requirements

### Requirement: 仅依赖公共聊天室单通道互动

**Reason**: 单一通道难以展示聊天软件核心能力，不利于课程作业的功能完整性。\
**Migration**: 迁移至“私聊会话 + 聊天连续性 + 状态感知 + 搜索入口”学习型功能组合。
