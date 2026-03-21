# Task 6 测试验收记录

## 1. 验收范围

- 对照 `.trae/specs/enable-real-interest-groups/checklist.md` 全项核对
- 补齐关键自动化测试：WebSocket 端点行为、登录拦截回归
- 记录当前未通过项与后续修复任务

## 2. 自动化测试补齐

- 新增 `GlobalChatEndpointTest`
  - 已加入用户可连接指定群组
  - 未加入用户会被拒绝并返回拒绝原因
  - 消息仅在同群组广播，不会跨群组串流
  - 消息落库 `room_id` 与连接房间一致
  - 超长消息触发策略拒绝，不落库
- 新增 `LoginInterceptorTest`
  - 已登录请求放行
  - 匿名页面请求重定向登录页
  - 匿名 API 请求返回 JSON 401

## 3. 环境执行结果

- 本地执行命令：`mvn test -DskipTests=false`
- 当前结果：环境缺少 Maven，可执行命令不存在，无法完成自动化套件实际跑通

## 4. Checklist 结论

- 已通过：9 项
- 未通过：1 项（兴趣分区创建与重名拦截）
- 阻塞项：Maven 环境缺失导致无法产出可执行测试报告

## 5. 后续任务

- 已在 `tasks.md` 新增 Task 7：
  - 增加兴趣分区创建能力与重名拦截
  - 补齐 Maven 环境后执行完整回归
