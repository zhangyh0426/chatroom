# BoardChat 社区聊天室

一个基于 `Spring MVC + MyBatis + JSP + Tomcat` 的本地社区项目，覆盖贴吧帖子、统一发帖页、用户中心、通知、全站搜索、兴趣群组和 WebSocket 聊天室等核心链路。

项目当前采用单体 `WAR` 部署模式，后端与页面模板都位于 `tieba/` 模块内，不存在额外的前端工程。

## 项目概览

- 内容社区：首页发现流、版块列表、帖子详情、楼层回复、楼中楼回复
- 发帖能力：统一发帖页、帖子类型、标签、图文上传、首图封面
- 用户系统：注册、登录、退出、个人资料、头像上传、我的帖子、我的回复
- 社区互动：帖子点赞、通知中心、全站搜索
- 实时聊天：全站大厅、兴趣群组、房间加入、历史消息、WebSocket 推送
- 兼容策略：兼容 `forum_user_profile.avatar` 和 `forum_user_profile.avatar_path` 两种历史字段

## 技术栈

- Java `21`
- Spring MVC `5.3.x`
- MyBatis `3.5.x`
- JSP + JSTL
- MySQL `8.x`
- Tomcat `9.x`
- Maven Wrapper
- WebSocket `javax.websocket`

## 仓库结构

```text
F:\zhao\chatroom\
├─ tieba\                      # 主应用（WAR）
│  ├─ src\main\java\           # Controller / Service / Mapper / WebSocket
│  ├─ src\main\resources\      # Spring / MyBatis / application.properties
│  ├─ src\main\webapp\         # JSP、静态资源、web.xml
│  └─ src\test\java\           # 单元测试与契约测试
├─ sql\                        # 建库脚本与增量迁移脚本
├─ data\uploads\               # 头像、帖子图片上传目录
├─ docs\                       # 设计文档与环境文档
├─ start.bat                   # 构建、部署、启动一体化脚本
├─ TECHNICAL_DESIGN.md         # 技术设计说明
└─ guestbook.sql               # 历史库导出文件
```

## 运行前提

- JDK 21 已安装并加入 `PATH`
- MySQL 8.x 可访问
- Tomcat 9.x 已安装
- Windows PowerShell / CMD 环境

推荐先看环境文档：

- [docs/ENVIRONMENT_SETUP.md](/F:/zhao/chatroom/docs/ENVIRONMENT_SETUP.md)

## 快速开始

### 1. 初始化数据库

推荐使用默认库名 `tieba`，这样无需修改应用默认 JDBC 配置。

1. 执行 [sql/schema.sql](/F:/zhao/chatroom/sql/schema.sql)
2. 检查是否存在 `forum_like_log`
3. 如果不存在，再按环境文档中的补丁 SQL 创建该表

说明：

- [sql/schema.sql](/F:/zhao/chatroom/sql/schema.sql) 已包含当前版本的大部分核心表、版块种子数据、兴趣分区和聊天室房间种子数据
- `v1.2`、`v1.3`、`v1.4` 主要用于老库增量升级，不是全新建库的首选入口

### 2. 配置应用

编辑 [tieba/src/main/resources/application.properties](/F:/zhao/chatroom/tieba/src/main/resources/application.properties)：

```properties
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://127.0.0.1:3306/tieba?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
jdbc.username=root
jdbc.password=123456
upload.path=F:/zhao/chatroom/data/uploads
chat.history.limit=50
chat.rate.limit.per.minute=20
chat.message.max.length=200
chat.sensitive.words=傻逼,法轮功,反共,赌博
```

至少确认这几项：

- `jdbc.url`
- `jdbc.username`
- `jdbc.password`
- `upload.path`

### 3. 构建与测试

在项目根目录运行：

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd test
.\mvnw.cmd clean package
```

### 4. 启动应用

回到仓库根目录运行：

```powershell
cd F:\zhao\chatroom
start.bat
```

`start.bat` 会自动执行以下动作：

1. 调用 Maven Wrapper 进行 `clean package`
2. 将 `tieba.war` 复制到 Tomcat `webapps`
3. 启动 Tomcat
4. 轮询 `http://localhost:8080/tieba`
5. 就绪后自动打开浏览器

### 5. 访问入口

- 首页：`http://localhost:8080/tieba`
- 登录：`http://localhost:8080/tieba/auth/login`
- 注册：`http://localhost:8080/tieba/auth/register`
- 全站搜索：`http://localhost:8080/tieba/search`
- 个人中心：`http://localhost:8080/tieba/user/profile`
- 聊天大厅：`http://localhost:8080/tieba/chat/global`
- 兴趣群组：`http://localhost:8080/tieba/chat/rooms`

## 常用命令

### 仅运行测试

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd test
```

### 完整打包

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd clean package
```

### 一键构建并启动

```powershell
cd F:\zhao\chatroom
start.bat
```

## 关键配置与行为

### 上传目录

- 根配置：`upload.path`
- 头像上传目录：`${upload.path}/avatars/<userId>/`
- 帖子图片目录：`${upload.path}/thread-images/<userId>/`
- 静态映射：`/uploads/** -> file:${upload.path}/`

### 上传限制

- 头像单文件上限：`2MB`
- 帖子图片单文件上限：`5MB`
- 单帖图片数量上限：`9`
- Servlet 请求总大小上限：`6MB`

### 聊天配置

- 历史消息返回数量：`chat.history.limit`
- 每分钟发言限流：`chat.rate.limit.per.minute`
- 单条消息最大长度：`chat.message.max.length`
- 敏感词列表：`chat.sensitive.words`
- WebSocket 端点：`/ws/chat/rooms/{roomCode}`

## 测试与验证建议

建议每次改动后至少执行：

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd test
.\mvnw.cmd clean package
```

然后再执行：

```powershell
cd F:\zhao\chatroom
start.bat
```

启动后至少人工检查以下页面：

- 首页发现流
- 某个版块页
- 帖子详情页
- 登录页
- 个人中心
- 发帖页
- 全站大厅或兴趣群组页

## 已知注意事项

### 1. `schema.sql` 与 `tieba_local_schema.sql` 不是同一条初始化路径

- `schema.sql` 默认创建数据库 `tieba`
- `tieba_local_schema.sql` 默认创建数据库 `tieba_local`
- 当前应用默认 JDBC 指向 `tieba`
- 两者字段细节略有差异，但代码做了部分历史兼容

不要混用后忘记修改 `jdbc.url`。

### 2. 点赞功能依赖 `forum_like_log`

当前仓库中的全量脚本与增量脚本有历史分叉：

- 代码已经包含 `LikeLogMapper`
- 但 [sql/schema.sql](/F:/zhao/chatroom/sql/schema.sql) 默认未创建 `forum_like_log`

因此新环境初始化时，请按环境文档补上该表。

### 3. `start.bat` 依赖外部 Tomcat

脚本会按以下顺序寻找 Tomcat：

1. `TOMCAT_HOME`
2. `CATALINA_HOME`
3. `F:\zhao\chatroom\tomcat`
4. `F:\zhao\chatroom\apache-tomcat`
5. `F:\zhao\chatroom\apache-tomcat*`

### 4. Tomcat 已运行时重复执行 `start.bat`

如果 Tomcat 已在运行，再次执行 `start.bat` 可能产生端口冲突或重复部署日志。建议先关闭旧实例，再重新执行。

## 相关文档

- [docs/ENVIRONMENT_SETUP.md](/F:/zhao/chatroom/docs/ENVIRONMENT_SETUP.md)
- [TECHNICAL_DESIGN.md](/F:/zhao/chatroom/TECHNICAL_DESIGN.md)
- [docs/IMPLEMENTATION_TASKS.md](/F:/zhao/chatroom/docs/IMPLEMENTATION_TASKS.md)
