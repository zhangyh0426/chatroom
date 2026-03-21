# 头像字段自动扫描与修复 Spec

## Why
当前已修复头像上传链路，但代码库仍可能残留 `avatar`/`avatar_path` 混用 SQL，存在潜在运行时爆错。需要一次自动扫描并修复同类风险，降低后续故障概率。

## What Changes
- 增加针对 Mapper SQL 的头像字段自动扫描能力
- 批量修复扫描到的 `avatar`/`avatar_path` 不一致用法
- 对查询语句引入兼容读策略，避免旧 schema 下读取失败
- 补充回归验证，覆盖核心模块（帖子、回复、聊天、用户信息）
- 输出扫描与修复结果清单，便于后续治理

## Impact
- Affected specs: SQL 兼容性治理、头像展示稳定性、回归质量保障
- Affected code: `src/main/resources/mapper/*.xml`、相关 service 校验逻辑、测试代码与文档清单

## ADDED Requirements
### Requirement: Mapper Avatar Column Scanner
系统 SHALL 支持对全部 MyBatis XML Mapper 执行头像字段自动扫描，并识别不兼容列引用。

#### Scenario: Detect inconsistent avatar column references
- **WHEN** 扫描器遍历 Mapper SQL
- **THEN** 输出包含文件路径、SQL 片段、建议修复策略的结果

### Requirement: Compatible Query Path
系统 SHALL 在受影响查询中提供兼容读取能力，确保不同 schema 下均可返回头像字段。

#### Scenario: Legacy schema read
- **WHEN** 数据库仅存在 `avatar`
- **THEN** 查询仍可返回应用层统一字段 `avatarPath`

#### Scenario: New schema read
- **WHEN** 数据库存在 `avatar_path`
- **THEN** 查询优先读取 `avatar_path`，并保持返回结构不变

## MODIFIED Requirements
### Requirement: Avatar Data Access Consistency
所有涉及头像字段的 SQL 访问必须遵循“主列优先 + 兼容回退 + 别名统一输出”的一致性规则，不允许单点硬编码导致跨环境失败。

## REMOVED Requirements
### Requirement: 手工逐文件修复头像字段
**Reason**: 人工排查易遗漏且成本高，无法持续保障一致性。  
**Migration**: 使用自动扫描结果驱动批量修复，并通过回归检查确认无残留风险。
