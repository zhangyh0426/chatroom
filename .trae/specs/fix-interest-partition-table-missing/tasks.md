# Tasks

- [x] Task 1: 对齐分区表迁移脚本与执行说明
  - [x] SubTask 1.1: 核对基础脚本与增量脚本是否都包含 forum_interest_partition
  - [x] SubTask 1.2: 明确推荐执行顺序与幂等执行方式
  - [x] SubTask 1.3: 增加缺表排查步骤与 SQL 校验命令

- [x] Task 2: 增加启动或请求前缺表自检能力
  - [x] SubTask 2.1: 在启动检查器增加 forum_interest_partition 存在性检查
  - [x] SubTask 2.2: 缺表时输出可操作错误信息（含迁移提示）
  - [x] SubTask 2.3: 保证不影响现有非聊天模块启动流程

- [x] Task 3: 完善分组页查询降级与“重试加载”
  - [x] SubTask 3.1: 捕获分区查询缺表异常并返回可读错误
  - [x] SubTask 3.2: 页面展示“重试加载”按钮并保持用户上下文
  - [x] SubTask 3.3: 补表后重试可恢复正常列表渲染

- [x] Task 4: 补齐测试与验收
  - [x] SubTask 4.1: 增加分区查询成功/缺表降级单测
  - [x] SubTask 4.2: 增加重试加载交互回归测试
  - [x] SubTask 4.3: 执行测试并记录验收结果

- [x] Task 5: 修复全量回归阻塞并重新验收
  - [x] SubTask 5.1: 修复 AvatarMapperCompatibilityScanTest 的别名兼容失败
  - [x] SubTask 5.2: 修复 UserServiceImplAvatarUpdateTest 的 Mockito 严格桩错误
  - [x] SubTask 5.3: 全量回归通过后回填 checklist 最后一项

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3
- Task 5 depends on Task 4
