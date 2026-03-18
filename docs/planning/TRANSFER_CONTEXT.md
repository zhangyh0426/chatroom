# 项目上下文转移文档 (TRANSFER_CONTEXT.md)
这是为新对话窗口准备的上下文记忆恢复文档。

## 1. 当前项目状态及环境认知
- **框架环境**: 单机 Tomcat 9 + Spring MVC 5 + MyBatis 3 + JSP (无 Spring Boot，无前后端分离)。
- **部署模式**: WAR 包部署，JDK 21。
- **构建工具**: Maven (尚未添加 Maven Wrapper，pom.xml 已包含必要依赖如 Jackson, BCrypt)。
- **数据库**: MySQL 8.x，主库名称约定为 `tieba_local`。
- **特别注意**: Spring 配置 `applicationContext.xml` 当前引入的是 `jdbc.properties`，但 `application.properties` 中定义的 `tieba_local` 才是正确的最新结构，这点需要在后续联调前修复配置。

## 2. 已完成的操作 (前置规划与底层搭建)
在切换到 `build` 模式前，我已完成了对项目源码的全面探勘，并完成了 **v1.1 (学生作业版)** 增强功能的核心底层建设：

1. **数据库增量变更 (v1.1_migration.sql)**:
   - 已在 `sql/v1.1_migration.sql` 编写了新增 `like_count` 字段及 `forum_like_log` 点赞记录表（复合唯一索引防刷赞）的 DDL 脚本。
2. **底层引擎增强 (Entity / VO / Mapper)**:
   - 创建了通用分页返回对象 `com.chatroom.tieba.vo.PageResult<T>`。
   - `ForumThread`, `ForumPost` 实体类增加了 `likeCount` 字段。
   - `ThreadVO` 增加了 `boardName`，`PostVO` 增加了 `threadTitle`，以支持跨表显示。
   - 修正了原有 `UserProfile` 实体中 `avatar` 到 `avatarPath` 的字段映射 bug。
3. **复杂 SQL 注入 (Mapper.xml)**:
   - 在 `ThreadMapper.xml` 和 `PostMapper.xml` 中，已手写支持了：**带条件的分页查询 (`LIMIT offset, size`)**、**热门榜单混合排序 (`view_count + reply_count * 10`)**、**防负数点赞安全更新 (`GREATEST`)**、以及 **我的足迹（Left Join 多表联查）** 的底层 SQL。
   - 创建了全新的 `LikeLogMapper` 及对应的 XML 用于点赞查重。

## 3. 下一步开发计划 (Pending Tasks)

当前我们正处于 **阶段3** 开始的节点，以下是需要在新窗口中继续执行的计划表：

- [ ] **阶段3: 个人中心与头像上传** (高优)
  - `spring-mvc.xml` 中配置 `CommonsMultipartResolver` 或 Servlet 3.0 Standard 解析器。
  - 创建 `UserController`，新增 `/user/profile` 和 `/api/user/profile/update` 接口。
  - 编写 `profile.jsp`，实现资料编辑与本地图片上传（需配置 Tomcat 虚拟目录 `/uploads` 映射到本地宿主机）。
- [ ] **阶段4/2: 业务层分页支撑与足迹查询** (高优)
  - `ForumService` 增加分页查询方法返回 `PageResult<ThreadVO>`。
  - 改造 `BoardController` 与 `board.jsp` 接入前端分页组件。
  - 在 `UserController` 中实现“我发布的帖子”、“我回复的帖子”分页查询及前端展示。
- [ ] **阶段5: AJAX 异步点赞** (亮点)
  - 创建点赞接口，验证 `forum_like_log` 防止重复点赞。
  - 改造帖子详情页视图，使用纯原生 JS 或 Fetch API 发起异步点赞请求，无刷新更新数字。
- [ ] **阶段6: 浏览量统与站内搜索** (中优)
  - `ThreadController` 中完善每次查看帖子时 `view_count + 1` 的逻辑。
  - `Header` 导航栏增加全局搜索框，进入全新的 `/search` 页面展示模糊匹配结果。
- [ ] **阶段7: 首页侧边栏 Top10 热榜** (中优)
  - `IndexController` 拉取 `ThreadMapper.findHotThreads(10)`，在 `index.jsp` 侧边栏进行渲染展示。
- [ ] **阶段8: 框架配置修复与联调** (收尾)
  - 修正 `jdbc.properties` 使其指向正确的 `tieba_local` 数据库，解决历史遗留的冲突。

---
**给在新窗口接手的 Antigravity 的提示**: 
请直接读取此文档，然后从“阶段3: 个人中心与头像上传”开始编写 Controller 和 JSP 代码，并执行所需的文件系统修改。
