# Tasks

- [x] Task 1: 修复兴趣分组入口点击链路并补齐前端错误反馈
  - [x] SubTask 1.1: 统一首页、导航、列表卡片的跳转地址与交互行为
  - [x] SubTask 1.2: 在入口与列表页增加失败提示、重试按钮与空态展示
  - [x] SubTask 1.3: 增加入口点击到目标页的回归测试用例

- [x] Task 2: 设计并落地兴趣分区数据模型与查询接口
  - [x] SubTask 2.1: 新增兴趣分区表结构与索引约束
  - [x] SubTask 2.2: 新增分区实体、Mapper、Service 查询能力
  - [x] SubTask 2.3: 完成分区列表接口并输出给分组页使用

- [x] Task 3: 设计并落地兴趣分组创建能力
  - [x] SubTask 3.1: 新增分组创建接口与参数校验规则
  - [x] SubTask 3.2: 实现“分组必须归属分区”的服务端校验
  - [x] SubTask 3.3: 在前端提供创建分组表单与成功后刷新流程

- [x] Task 4: 完善加入兴趣分组流程与状态渲染
  - [x] SubTask 4.1: 保证加入接口幂等与错误提示可读
  - [x] SubTask 4.2: 详情页按加入状态切换输入区与按钮
  - [x] SubTask 4.3: 增加加入前后状态切换的联调验证

- [x] Task 5: 优化群组详情页连接稳定性与可操作反馈
  - [x] SubTask 5.1: 完善连接状态机（在线/重连中/失败/拒绝）
  - [x] SubTask 5.2: 优化重连策略、重连上限与输入区禁用逻辑
  - [x] SubTask 5.3: 区分系统消息与用户消息展示样式

- [x] Task 6: 完成测试与验收
  - [x] SubTask 6.1: 补齐 Service/Controller 关键单测
  - [x] SubTask 6.2: 补齐前端关键交互回归清单与人工验收记录
  - [x] SubTask 6.3: 验证不影响 GLOBAL 兼容入口与现有登录拦截

- [x] Task 7: 修复验收阻塞项并完成自动化回归
  - [x] SubTask 7.1: 增加兴趣分区创建与重名拦截能力
  - [x] SubTask 7.2: 补齐 Maven 执行环境后运行完整测试套件

# Task Dependencies

- Task 3 depends on Task 2
- Task 4 depends on Task 3
- Task 5 depends on Task 4
- Task 6 depends on Task 1
- Task 6 depends on Task 5
