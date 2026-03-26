# Tasks
- [x] Task 1: 设计并接入个人中心“我要发帖”入口
  - [x] SubTask 1.1: 在个人中心页面增加发帖入口与状态展示
  - [x] SubTask 1.2: 复用现有登录判定策略处理未登录引导
  - [x] SubTask 1.3: 确认入口跳转路径与现有路由体系一致

- [x] Task 2: 打通发帖页面与提交链路
  - [x] SubTask 2.1: 提供发帖表单页并复用既有字段规范
  - [x] SubTask 2.2: 在 Controller 层接入发帖提交与参数校验
  - [x] SubTask 2.3: 在 Service 层复用发帖业务并返回成功落点

- [x] Task 3: 融合发帖成功后的足迹可见性
  - [x] SubTask 3.1: 确认新帖进入“我发的帖子”查询链路
  - [x] SubTask 3.2: 校验排序、分页与既有足迹展示行为一致
  - [x] SubTask 3.3: 完成发帖后跳转与个人中心回流体验

- [x] Task 4: 完成回归验证与规范收口
  - [x] SubTask 4.1: 覆盖未登录发帖引导与登录后返回目标
  - [x] SubTask 4.2: 覆盖发帖成功与我的足迹联动场景
  - [x] SubTask 4.3: 覆盖参数非法与错误提示一致性

- [x] Task 5: 将发帖入口迁移到首页并保持入口一致性
  - [x] SubTask 5.1: 在首页接入“我要发帖”入口与登录态展示
  - [x] SubTask 5.2: 调整个人中心入口策略以匹配首页主入口
  - [x] SubTask 5.3: 确认首页入口跳转发帖页参数与路由正确

- [x] Task 6: 执行首页发帖全链路验证并补齐回归
  - [x] SubTask 6.1: 覆盖首页发帖入口在登录态与未登录态行为
  - [x] SubTask 6.2: 覆盖发帖成功、详情回流与我的足迹可见性
  - [x] SubTask 6.3: 执行全量测试并确认不破坏既有论坛能力

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 1
- Task 4 depends on Task 2
- Task 4 depends on Task 3
- Task 6 depends on Task 5
