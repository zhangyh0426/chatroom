# Tasks
- [x] Task 1: 定位并确认头像字段 schema 差异与现有 SQL 映射不一致点
  - [x] SubTask 1.1: 审查 `UserProfileMapper.xml` 与用户资料实体字段映射
  - [x] SubTask 1.2: 核对目标数据库 `forum_user_profile` 表头像相关列
  - [x] SubTask 1.3: 明确主列与兼容列命名策略及优先级

- [x] Task 2: 实现头像更新 SQL 兼容写入策略并保持事务一致性
  - [x] SubTask 2.1: 改造 mapper 更新语句支持主路径与回退路径
  - [x] SubTask 2.2: 在 service 层封装异常分流与回退触发条件
  - [x] SubTask 2.3: 保证失败时返回一致业务错误且不产生脏状态

- [x] Task 3: 优化错误处理与可观测性
  - [x] SubTask 3.1: 为 SQL 语法错误补充结构化日志上下文
  - [x] SubTask 3.2: 输出用户可理解的失败提示并保留内部诊断信息
  - [x] SubTask 3.3: 防止敏感信息泄露到前端响应

- [x] Task 4: 增加 schema 前置检查与验证
  - [x] SubTask 4.1: 在启动期或健康检查中校验头像字段存在性
  - [x] SubTask 4.2: 缺失时输出高优先级告警并进入兼容模式
  - [x] SubTask 4.3: 补充验证用例覆盖主路径、回退路径、失败路径

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 2
