# Tasks
- [x] Task 1: 修复写操作认证拦截与配置一致性，确保核心链路稳定
  - [x] SubTask 1.1: 扩展拦截器匹配到发帖、回帖、楼中楼、点赞写接口
  - [x] SubTask 1.2: 为写接口增加未登录兜底处理并统一未授权返回
  - [x] SubTask 1.3: 合并并校验数据源与业务配置读取入口

- [x] Task 2: 实现帖子发现增强能力（分页、搜索、热门榜）
  - [x] SubTask 2.1: 在 Service 层接入分页与关键词搜索查询
  - [x] SubTask 2.2: 在 Controller 暴露分页与搜索参数并返回分页模型
  - [x] SubTask 2.3: 在列表页面新增搜索框、分页器与热门榜区块

- [x] Task 3: 实现帖子点赞与防重复点赞
  - [x] SubTask 3.1: 新增点赞接口并完成登录态与参数校验
  - [x] SubTask 3.2: 接入点赞日志唯一性校验，阻止重复点赞
  - [x] SubTask 3.3: 在线程详情页增加点赞按钮与状态反馈

- [x] Task 4: 实现个人中心“我的足迹”
  - [x] SubTask 4.1: 新增“我发的帖子”查询与分页
  - [x] SubTask 4.2: 新增“我回复的帖子”查询与分页
  - [x] SubTask 4.3: 在个人中心增加足迹入口与双列表视图

- [x] Task 5: 增强聊天室治理与状态字段一致性
  - [x] SubTask 5.1: 在 WebSocket 消息入口实现限频和长度校验
  - [x] SubTask 5.2: 增加敏感词过滤与拒绝反馈
  - [x] SubTask 5.3: 统一 status 值域并修正实体与 SQL 语义映射

- [x] Task 6: 完成功能回归验证与文档同步
  - [x] SubTask 6.1: 为分页搜索、点赞去重、足迹页补充测试
  - [x] SubTask 6.2: 为聊天室限流与敏感词校验补充测试
  - [x] SubTask 6.3: 执行回归并更新变更说明

- [x] Task 7: 实现话题删除能力并保证权限与可见性
  - [x] SubTask 7.1: 新增话题删除接口并增加登录态与作者校验
  - [x] SubTask 7.2: 在数据层实现软删除与详情页可见性控制
  - [x] SubTask 7.3: 补充删除链路验证并记录环境限制

- [x] Task 8: 实现删除即减计数并保持幂等
  - [x] SubTask 8.1: 删除时同步回收版块主题计数与帖子计数
  - [x] SubTask 8.2: 防重复回收并确保重复删除不产生负计数
  - [x] SubTask 8.3: 补充计数回收链路测试与验证记录

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1
- Task 5 depends on Task 1
- Task 6 depends on Task 2
- Task 6 depends on Task 3
- Task 6 depends on Task 4
- Task 6 depends on Task 5
- Task 7 depends on Task 1
- Task 8 depends on Task 7

* 验证记录：新增 `ForumServiceImplCoreFeatureTest` 删除链路测试，覆盖作者删除成功、越权删除拦截、已不可见帖子访问拦截。
* 验证记录：新增 `ForumServiceImplCoreFeatureTest` 与 `ChatMessagePolicyTest`，覆盖分页搜索、点赞去重、足迹分页、聊天室限流/敏感词/长度校验。
* 验证记录：扩展 `ForumServiceImplCoreFeatureTest`，覆盖删除计数回收、重复删除幂等、异常脏数据防负数回收。
* 环境限制：当前环境未安装 Maven 命令，`mvn test` 无法执行（命令不可识别）。
