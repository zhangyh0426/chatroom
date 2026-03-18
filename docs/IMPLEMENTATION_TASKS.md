# 本地贴吧系统开发任务拆解

## 阶段 1：环境准备

- 安装本地 MySQL 并创建 `tieba_local`
- 执行 [tieba_local_schema.sql](/F:/zhao/chatroom/sql/tieba_local_schema.sql)
- 校正 [application.properties](/F:/zhao/chatroom/tieba/src/main/resources/application.properties) 中的数据库账号、密码和上传目录
- 在本机补齐 Maven 或 Maven Wrapper

## 阶段 2：基础工程

- 完善 Spring MVC 配置
- 增加统一异常处理
- 增加登录拦截器
- 增加字符编码过滤器
- 增加日志配置

## 阶段 3：用户域

- 实现注册、登录、退出
- 实现密码加密
- 实现用户资料维护
- 实现角色与权限判断
- 实现封禁校验

## 阶段 4：贴吧域

- 实现分区管理
- 实现吧管理
- 实现主题帖发布和编辑
- 实现楼层回复
- 实现楼中楼回复
- 实现帖子列表和详情分页

## 阶段 5：公告与后台

- 实现公告发布与上下线
- 实现帖子审核
- 实现用户管理
- 实现聊天消息管理
- 实现审核日志展示

## 阶段 6：聊天室

- 将 [GlobalChatEndpoint.java](/F:/zhao/chatroom/tieba/src/main/java/com/chatroom/tieba/websocket/GlobalChatEndpoint.java) 从演示广播改为业务实现
- 接入数据库消息落库
- 接入登录态校验
- 接入敏感词与频率限制
- 接入禁言逻辑
- 提供最近消息历史查询接口

## 阶段 7：收尾与验收

- 完善 JSP 页面样式
- 完善 SQL 索引与统计字段更新逻辑
- 补齐集成测试和冒烟测试
- 打包 `WAR` 并部署到本机 Tomcat
- 验证首页、帖子、后台、聊天室全链路

## 当前产物

- 技术文档：[TECHNICAL_DESIGN.md](/F:/zhao/chatroom/TECHNICAL_DESIGN.md)
- 建库脚本：[tieba_local_schema.sql](/F:/zhao/chatroom/sql/tieba_local_schema.sql)
- Tomcat 工程骨架：[tieba](/F:/zhao/chatroom/tieba)
