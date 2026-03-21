# Tasks
- [x] Task 1: 自动扫描全部 Mapper 中头像列引用并生成问题清单
  - [x] SubTask 1.1: 扫描 `src/main/resources/mapper` 下所有 XML
  - [x] SubTask 1.2: 识别 `avatar` 与 `avatar_path` 的不一致读写语句
  - [x] SubTask 1.3: 产出待修复文件列表与修复策略

- [x] Task 2: 批量修复头像相关查询 SQL 的列兼容问题
  - [x] SubTask 2.1: 修复帖子与回复查询中的头像字段引用
  - [x] SubTask 2.2: 修复聊天与用户相关查询中的头像字段引用
  - [x] SubTask 2.3: 统一返回字段别名为 `avatar_path`

- [x] Task 3: 完成回归验证并更新治理清单
  - [x] SubTask 3.1: 增加或更新测试覆盖主列路径与兼容路径
  - [x] SubTask 3.2: 执行可用验证命令并记录环境限制
  - [x] SubTask 3.3: 检查并确认无遗漏的头像字段不一致点

- 验证记录：新增 `AvatarMapperCompatibilityScanTest`，覆盖主列与兼容列别名输出检查。
- 环境限制：当前环境缺少 Maven 可执行文件，`mvn test` 无法执行（命令不可识别）。

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
