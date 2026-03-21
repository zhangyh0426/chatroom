# 修复兴趣分区表缺失 Spec

## Why
线上/本地环境在访问兴趣分组页时出现 `forum_interest_partition` 表不存在错误，导致页面无法加载。  
当前代码已依赖分区查询，但数据库迁移未在所有环境一致落地，需要补齐兼容与兜底策略。

## What Changes
- 补齐数据库迁移执行路径，确保 `forum_interest_partition` 在目标环境可用。
- 增加应用启动阶段的聊天分区表自检与缺失提示，避免请求时才暴露 SQL 异常。
- 在兴趣分组列表查询链路增加降级兜底：分区表缺失时返回可读错误并支持“重试加载”。
- 补充运维执行说明：初始化脚本、增量迁移脚本的推荐顺序与回滚策略。
- 增加回归测试，覆盖“表存在/表缺失”两种路径。

## Impact
- Affected specs: 兴趣分组列表可用性、数据库迁移一致性、错误提示与重试体验
- Affected code: `sql/*.sql`、`InterestPartition*`、`ChatController`、启动检查器、相关测试

## ADDED Requirements
### Requirement: 兴趣分区表可用性保障
系统 SHALL 在部署后保证 `forum_interest_partition` 可被正常查询。

#### Scenario: 迁移后正常查询
- **WHEN** 环境已执行基础脚本与增量迁移
- **THEN** `SELECT * FROM forum_interest_partition ...` 成功返回结果

#### Scenario: 缺表快速识别
- **WHEN** 环境缺失 `forum_interest_partition`
- **THEN** 启动检查或首个请求可返回明确错误“请执行迁移脚本”

### Requirement: 分组页缺表降级与重试
系统 SHALL 在分区表缺失或查询异常时提供可读错误并支持重试加载。

#### Scenario: 查询失败时提示
- **WHEN** 分区查询抛出 SQLSyntaxErrorException
- **THEN** 页面显示可读错误文案与“重试加载”操作

#### Scenario: 补表后重试成功
- **WHEN** 运维补齐分区表并点击“重试加载”
- **THEN** 页面恢复正常展示分区与群组列表

## MODIFIED Requirements
### Requirement: 兴趣分组列表加载
系统原有“直接查询分区表并渲染”修改为“先自检/可降级渲染”，避免直接 500 页面。

## REMOVED Requirements
### Requirement: 仅依赖代码升级自动可用
**Reason**: 该能力依赖数据库结构，单纯代码发布无法保证查询可用。  
**Migration**: 发布前必须执行并核验 `forum_interest_partition` 相关迁移脚本。
