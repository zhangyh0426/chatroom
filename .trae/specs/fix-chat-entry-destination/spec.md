# 聊天室入口落点修复 Spec

## Why
当前“进入聊天室”入口落到 `/chat/rooms` 后，页面首屏先展示“创建兴趣分组”表单，用户误以为进入了创建分区页面。需要让入口优先落在可浏览/可进入的聊天室列表区。

## What Changes
- 将聊天室列表区设为页面首屏主区域，创建分组区降级为次级操作区。
- 所有“进入聊天室”前端入口统一指向聊天室列表锚点（`/chat/rooms#rooms-lobby`）。
- 在列表区增加清晰标题与说明，确保用户理解这是“进入聊天室”后的落点页面。

## Impact
- Affected specs: 聊天室入口可发现性、聊天室落点一致性、创建流程可达性
- Affected code: `WEB-INF/jsp/chat/rooms.jsp`、`WEB-INF/jsp/common/header.jsp`、`WEB-INF/jsp/index.jsp`

## ADDED Requirements
### Requirement: 聊天室入口落点一致
系统 SHALL 在所有“进入聊天室”入口将用户带到聊天室列表区域，而非创建表单区域。

#### Scenario: 从首页入口进入聊天室
- **WHEN** 用户点击首页或导航中的“进入聊天室/聊天室”入口
- **THEN** URL 应为 `/chat/rooms#rooms-lobby`
- **AND** 首屏可见聊天室列表区域标题与房间列表/空态信息

### Requirement: 创建分组为次级操作
系统 SHALL 将“创建兴趣分组”作为次级管理操作展示，不应抢占首屏主流程。

#### Scenario: 用户进入聊天室页面
- **WHEN** 页面加载完成
- **THEN** 首先展示“聊天室列表/兴趣群组”区域
- **AND** 创建分组区域可继续使用，但位于列表区之后

## MODIFIED Requirements
### Requirement: 聊天室页面信息架构
聊天室页面默认信息架构调整为“先浏览可进入房间，再执行创建分组管理操作”，以匹配“进入聊天室”的用户意图。

## REMOVED Requirements
### Requirement: 首屏优先展示创建分组表单
**Reason**: 与“进入聊天室”的主任务不一致，造成入口理解偏差。  
**Migration**: 保留原创建能力，仅调整展示顺序并统一入口锚点。
