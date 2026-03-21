# 核心话题能力增强 Spec

## Why
当前话题链路已覆盖发布、浏览、点赞、评论，但删除能力与计数一致性是完整产品闭环的关键。  
需要在不破坏既有能力的前提下，补齐“作者删除 + 可见性控制 + 删除即减计数 + 幂等防负数”。

## What Changes
- 统一写接口登录态校验，避免未登录写操作异常
- 增强话题发现：分页、关键词搜索、热门榜
- 提供线程级点赞并防重复计数
- 增加个人中心“我发的帖子/我回复的帖子”
- 增强聊天室消息治理（限频、长度、敏感词）
- 新增作者删除话题能力（软删除 `status=0`）
- 新增删除即减计数：删除成功后回收 `board.thread_count` 与 `board.post_count`
- 增加删除幂等与防负数策略，避免重复删除或脏数据导致计数异常

## Impact
- Affected specs: 认证与鉴权、话题生命周期、互动计数一致性、个人中心足迹、聊天室治理
- Affected code: `controller/*`、`service/*`、`mapper/*`、`resources/mapper/*`、`src/test/*`

## ADDED Requirements
### Requirement: Author Thread Deletion
The system SHALL provide author-only thread deletion through soft-delete semantics.

#### Scenario: Author deletes own thread
- **WHEN** 已登录作者删除自己发布的话题
- **THEN** 话题状态更新为不可见，详情页不可访问，列表不再展示

#### Scenario: Non-author deletion rejected
- **WHEN** 非作者用户请求删除话题
- **THEN** 系统拒绝请求并返回权限错误

### Requirement: Counter Recycle On Deletion
The system SHALL recycle board counters when a visible thread is successfully deleted.

#### Scenario: Recycle board counters after successful deletion
- **WHEN** 作者成功删除可见话题
- **THEN** `board.thread_count` 减 1，`board.post_count` 按话题回复量回收

#### Scenario: Prevent negative counters
- **WHEN** 回收量异常或计数已接近 0
- **THEN** 计数不小于 0

### Requirement: Idempotent Deletion
The system SHALL keep deletion idempotent under repeated requests.

#### Scenario: Duplicate delete request
- **WHEN** 同一作者重复删除同一话题
- **THEN** 不重复回收计数且不产生负计数

## MODIFIED Requirements
### Requirement: Thread Visibility Contract
所有话题读取链路（详情、列表、足迹）必须仅返回 `status=1` 的可见数据，软删除后不允许通过常规页面访问。

## REMOVED Requirements
### Requirement: 删除后不处理冗余计数
**Reason**: 会导致版块计数与实际数据不一致。  
**Migration**: 改为删除成功即同步回收计数，并通过幂等与防负数策略保证稳定。
