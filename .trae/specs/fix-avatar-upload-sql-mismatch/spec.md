# 头像上传 SQL 字段兼容修复 Spec

## Why
当前头像上传在数据库更新阶段失败，直接导致核心用户功能不可用。根因是 MyBatis 映射 SQL 使用了 `avatar_path` 字段，但线上/本地实际表结构缺少该列，出现 SQLSyntaxError。

## What Changes
- 修复用户头像更新 SQL 与真实数据库表结构不一致问题
- 在数据访问层引入可回退兼容策略，避免因字段差异导致头像上传整体失败
- 优化异常分层与日志信息，区分 SQL 语法错误、参数错误、业务错误
- 完善头像上传链路校验与失败提示，确保用户可感知且可恢复
- 增加数据库结构前置检查与启动期告警，提前暴露 schema 漂移风险

## Impact
- Affected specs: 用户资料维护、头像上传、持久化兼容性、错误可观测性
- Affected code: `UserProfileMapper.xml`、用户资料 Service、头像上传 Controller、数据库初始化/迁移脚本、异常处理与日志模块

## ADDED Requirements
### Requirement: Avatar Update SQL Compatibility
系统 SHALL 在执行头像地址更新前确保所用列名与目标环境数据库 schema 一致，或使用可验证的回退策略完成更新。

#### Scenario: Schema matches primary column
- **WHEN** 用户上传头像并触发资料更新
- **THEN** 系统使用主列名执行更新并返回成功

#### Scenario: Primary column missing but fallback available
- **WHEN** 主列名更新触发 `Unknown column` 错误且检测到兼容列存在
- **THEN** 系统自动回退到兼容列完成更新，业务请求返回成功

### Requirement: Structured Error Handling for Avatar Upload
系统 SHALL 对头像上传失败进行结构化错误处理，向用户返回可理解提示，同时在服务端记录可定位上下文。

#### Scenario: SQL syntax error during update
- **WHEN** 更新语句触发 SQL 语法相关异常
- **THEN** 返回“头像更新失败，请联系管理员检查数据库结构”并记录 mapper、SQL 片段、用户标识

### Requirement: Schema Drift Early Warning
系统 SHALL 在应用启动或健康检查阶段校验关键字段存在性，并输出告警防止运行时故障。

#### Scenario: Required avatar column missing
- **WHEN** 启动检查发现头像字段不存在
- **THEN** 写入高优先级告警日志并标记系统处于降级兼容状态

## MODIFIED Requirements
### Requirement: Avatar Upload Success Path
系统在头像文件保存成功后，必须保证数据库头像字段更新与业务返回一致；若主更新路径失败，必须执行兼容回退并在同一请求内完成结果判定，不允许静默失败。

## REMOVED Requirements
### Requirement: 单一固定列名更新假设
**Reason**: 该假设忽略多环境 schema 演进差异，导致运行时不可恢复故障。  
**Migration**: 迁移到“主列 + 兼容列 + 启动检查”策略，并在后续统一数据库字段命名后移除兼容分支。
