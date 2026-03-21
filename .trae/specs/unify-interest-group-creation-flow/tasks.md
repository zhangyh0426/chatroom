# Tasks

- [x] Task 1: 统一缺表提示与重试加载文案
  - [x] SubTask 1.1: 对齐后端缺表错误码与提示消息
  - [x] SubTask 1.2: 前端展示固定迁移指引与重试加载按钮
  - [x] SubTask 1.3: 补充缺表场景接口与页面测试

- [x] Task 2: 归并兴趣分组创建入口
  - [x] SubTask 2.1: 移除或下线重复创建入口
  - [x] SubTask 2.2: 保留单一“创建兴趣分组”触发点
  - [x] SubTask 2.3: 校验入口权限与可见性规则一致

- [x] Task 3: 实现先分区后分组的统一创建流程
  - [x] SubTask 3.1: 在创建流程中先调用分区创建接口
  - [x] SubTask 3.2: 分区成功后串行调用分组创建接口
  - [x] SubTask 3.3: 分区失败时阻断分组创建并返回可读错误

- [x] Task 4: 实现创建成功即时生效
  - [x] SubTask 4.1: 创建成功后更新本地列表状态
  - [x] SubTask 4.2: 保证无需刷新即可看到新分组
  - [x] SubTask 4.3: 回归验证创建后后续操作可用

- [x] Task 5: 完成回归与验收
  - [x] SubTask 5.1: 执行相关单测与集成验证
  - [x] SubTask 5.2: 覆盖缺表提示、入口归并、即时生效三类主路径
  - [x] SubTask 5.3: 回填 checklist 验收结果

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 3
- Task 5 depends on Task 4
