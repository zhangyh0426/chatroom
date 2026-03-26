# 环境配置与部署指南

本文档面向当前仓库的实际运行方式，重点说明本地开发环境、数据库初始化、配置文件、Tomcat 部署和常见排障。

## 1. 适用范围

当前项目的真实运行形态是：

- 单体 `WAR` 工程
- 使用外部 `Tomcat 9`
- 数据库使用 `MySQL 8`
- 页面使用 `JSP + JSTL`
- 聊天功能使用 `WebSocket`
- 主要运行入口为 [start.bat](/F:/zhao/chatroom/start.bat)

## 2. 推荐环境

### 2.1 已验证组合

- JDK：`21`
- MySQL：`8.x`
- Tomcat：`9.x`
- 操作系统：`Windows`

### 2.2 依赖检查命令

```powershell
java -version
mysql --version
```

Tomcat 通常不需要加入 `PATH`，但至少要能通过以下任一方式被 `start.bat` 找到：

- 设置 `TOMCAT_HOME`
- 设置 `CATALINA_HOME`
- 将 Tomcat 放到仓库根目录约定位置

## 3. 关键配置文件

### 3.1 应用配置

文件位置：

- [tieba/src/main/resources/application.properties](/F:/zhao/chatroom/tieba/src/main/resources/application.properties)

当前默认内容：

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

### 3.2 Web 配置

- [tieba/src/main/webapp/WEB-INF/web.xml](/F:/zhao/chatroom/tieba/src/main/webapp/WEB-INF/web.xml)
- [tieba/src/main/resources/applicationContext.xml](/F:/zhao/chatroom/tieba/src/main/resources/applicationContext.xml)
- [tieba/src/main/resources/spring-mvc.xml](/F:/zhao/chatroom/tieba/src/main/resources/spring-mvc.xml)

这些文件决定了：

- Spring 容器加载方式
- HikariCP 数据源配置
- MyBatis Mapper 扫描
- 上传解析器
- `/static/**` 和 `/uploads/**` 映射
- 登录拦截器作用范围

## 4. 数据库初始化策略

当前仓库存在两条历史 SQL 线路，文档必须区分：

### 4.1 路线 A：默认库名 `tieba`

适用场景：

- 你希望沿用当前 `application.properties` 默认 JDBC 配置
- 你不想额外改库名

建议步骤：

1. 执行 [sql/schema.sql](/F:/zhao/chatroom/sql/schema.sql)
2. 补建 `forum_like_log`
3. 检查核心表和种子数据

说明：

- `schema.sql` 已包含大部分现行能力所需表结构
- 已包含版块、兴趣分区和聊天室房间种子数据
- 但默认不包含 `forum_like_log`

#### 4.1.1 补建 `forum_like_log`

在 `tieba` 数据库中执行：

```sql
USE `tieba`;

CREATE TABLE IF NOT EXISTS `forum_like_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '点赞用户ID',
  `target_id` bigint unsigned NOT NULL COMMENT '被点赞的内容ID',
  `target_type` varchar(20) NOT NULL COMMENT '内容类型: THREAD, POST',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_user_target` (`user_id`, `target_id`, `target_type`),
  KEY `idx_like_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_like_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `forum_user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';
```

#### 4.1.2 验证核心表

```sql
USE `tieba`;
SHOW TABLES LIKE 'forum_user_account';
SHOW TABLES LIKE 'forum_thread';
SHOW TABLES LIKE 'forum_interest_partition';
SHOW TABLES LIKE 'forum_chat_room';
SHOW TABLES LIKE 'forum_like_log';
```

### 4.2 路线 B：快照库 `tieba_local`

适用场景：

- 你想使用 [sql/tieba_local_schema.sql](/F:/zhao/chatroom/sql/tieba_local_schema.sql) 一次性建出一套更完整的快照结构
- 你愿意同步修改 JDBC 库名

注意：

- `tieba_local_schema.sql` 默认创建数据库 `tieba_local`
- 如果你选择这条路线，必须修改 `jdbc.url`

执行后请把：

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/tieba?...
```

改为：

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/tieba_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
```

补充说明：

- 这条路线下同样建议确认 `forum_like_log` 是否存在
- 如果不存在，也执行上一节的建表 SQL，只是数据库换成 `tieba_local`

## 5. 增量迁移脚本说明

仓库内还有若干增量脚本，主要面向历史库升级：

- [sql/v1.1_migration.sql](/F:/zhao/chatroom/sql/v1.1_migration.sql)
- [sql/v1.2_interest_partition_migration.sql](/F:/zhao/chatroom/sql/v1.2_interest_partition_migration.sql)
- [sql/v1.3_posting_page_bootstrap.sql](/F:/zhao/chatroom/sql/v1.3_posting_page_bootstrap.sql)
- [sql/v1.4_content_discovery_notifications.sql](/F:/zhao/chatroom/sql/v1.4_content_discovery_notifications.sql)

使用建议：

- 全新环境优先用 `schema.sql`
- 老库补丁时，再按缺失能力选择迁移脚本
- 不要把所有迁移脚本无差别串行执行到全新库上

### 5.1 各脚本用途

- `v1.1`：点赞能力相关，主要是 `forum_like_log`
- `v1.2`：兴趣分区与聊天室分区字段升级
- `v1.3`：统一发帖页所需的帖子图片和版块/分类引导数据
- `v1.4`：标签、通知、搜索与内容发现相关结构

## 6. 配置项说明

### 6.1 数据库

- `jdbc.driverClassName`
  说明：JDBC 驱动类名

- `jdbc.url`
  说明：数据库地址、库名、时区、编码

- `jdbc.username`
  说明：数据库账号

- `jdbc.password`
  说明：数据库密码

### 6.2 上传目录

- `upload.path`
  说明：文件上传根目录

实际子目录：

- 头像：`${upload.path}/avatars/<userId>/`
- 帖子图片：`${upload.path}/thread-images/<userId>/`

要求：

- 路径所在磁盘必须存在
- 运行 Tomcat 的账户必须有读写权限

### 6.3 聊天配置

- `chat.history.limit`
  说明：聊天室页面加载历史消息数量

- `chat.rate.limit.per.minute`
  说明：单用户每分钟允许发送的消息条数

- `chat.message.max.length`
  说明：单条聊天消息最大长度

- `chat.sensitive.words`
  说明：逗号分隔的敏感词集合

## 7. 上传与静态资源映射

项目在 [tieba/src/main/resources/spring-mvc.xml](/F:/zhao/chatroom/tieba/src/main/resources/spring-mvc.xml) 中定义了：

```xml
<mvc:resources mapping="/static/**" location="/static/" />
<mvc:resources mapping="/uploads/**" location="file:${upload.path}/" />
```

这意味着：

- JSP 中访问 `/static/**` 会映射到 WAR 内静态资源
- JSP 中访问 `/uploads/**` 会映射到本地磁盘上传目录

如果头像或帖子图片无法显示，优先检查：

1. `upload.path` 是否正确
2. 对应文件是否真的写入磁盘
3. Tomcat 是否有权限读取该目录

## 8. 上传限制

### 8.1 Servlet 层限制

来自 [tieba/src/main/webapp/WEB-INF/web.xml](/F:/zhao/chatroom/tieba/src/main/webapp/WEB-INF/web.xml)：

- `max-file-size = 5242880`，即单文件 `5MB`
- `max-request-size = 6291456`，即单请求 `6MB`

### 8.2 业务层限制

头像上传：

- 单文件上限 `2MB`
- 仅支持 `jpg/jpeg/png/gif/webp`

帖子图片上传：

- 单文件上限 `5MB`
- 单帖最多 `9` 张

## 9. Tomcat 配置与启动

### 9.1 `start.bat` 的 Tomcat 检测顺序

[start.bat](/F:/zhao/chatroom/start.bat) 会按以下顺序寻找 Tomcat：

1. `TOMCAT_HOME`
2. `CATALINA_HOME`
3. `F:\zhao\chatroom\tomcat`
4. `F:\zhao\chatroom\apache-tomcat`
5. `F:\zhao\chatroom\apache-tomcat*`

### 9.2 推荐做法

最稳妥的是显式设置用户级环境变量：

```powershell
[Environment]::SetEnvironmentVariable(
  "TOMCAT_HOME",
  "C:\Users\Simon\opt\tomcat\apache-tomcat-9.0.115",
  "User"
)
```

重新打开终端后验证：

```powershell
$env:TOMCAT_HOME
```

### 9.3 启动命令

```powershell
cd F:\zhao\chatroom
start.bat
```

### 9.4 停止命令

```powershell
& "$env:TOMCAT_HOME\bin\shutdown.bat"
```

如果没有设置环境变量，就把路径替换成你的实际 Tomcat 安装目录。

## 10. Maven 与测试

### 10.1 运行测试

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd test
```

### 10.2 完整构建

```powershell
cd F:\zhao\chatroom\tieba
.\mvnw.cmd clean package
```

### 10.3 产物位置

- WAR 文件：[tieba/target/tieba.war](/F:/zhao/chatroom/tieba/target/tieba.war)

## 11. 启动后验证

### 11.1 基础验证

打开：

- `http://localhost:8080/tieba`
- `http://localhost:8080/tieba/auth/login`
- `http://localhost:8080/tieba/search`

### 11.2 登录后验证

登录后检查：

- `http://localhost:8080/tieba/user/profile`
- `http://localhost:8080/tieba/board/post/thread`
- `http://localhost:8080/tieba/chat/global`
- `http://localhost:8080/tieba/chat/rooms`

### 11.3 WebSocket 说明

聊天使用的服务端端点是：

```text
/ws/chat/rooms/{roomCode}
```

例如全站大厅：

```text
/ws/chat/rooms/GLOBAL
```

聊天室建立连接前要求：

- 已登录
- 已加入对应群组

## 12. 常见问题

### 12.1 启动报数据库连接错误

优先检查：

- `jdbc.url` 的库名是否与实际初始化的数据库一致
- MySQL 端口是否是 `3306`
- 用户名密码是否正确

### 12.2 头像列是 `avatar` 还是 `avatar_path`

仓库当前存在历史兼容：

- 旧库可能是 `avatar`
- 新快照可能是 `avatar_path`

当前代码兼容这两种字段，不必为此立即重建数据库。

### 12.3 聊天室页面提示先执行 `v1.2`

这通常说明当前数据库缺少：

- `forum_interest_partition`
- `forum_chat_room.partition_id`

此时执行 [sql/v1.2_interest_partition_migration.sql](/F:/zhao/chatroom/sql/v1.2_interest_partition_migration.sql) 对老库升级。

### 12.4 启动时 Tomcat 日志出现端口冲突

常见原因：

- Tomcat 已经在运行
- 上一次实例没有正确关闭

处理方式：

1. 先执行 `shutdown.bat`
2. 确认 `8080` 和 `8005` 没被占用
3. 再重新执行 `start.bat`

### 12.5 上传成功但页面看不到图片

优先检查：

- `upload.path` 是否和实际写入目录一致
- 上传文件是否存在
- Tomcat 是否有权限读取该目录
- 页面访问的 URL 是否以 `/uploads/` 开头

## 13. 推荐初始化顺序

对于新机器，我建议直接按下面顺序做：

1. 安装 JDK 21、MySQL 8、Tomcat 9
2. 设置 Tomcat 路径
3. 执行 [sql/schema.sql](/F:/zhao/chatroom/sql/schema.sql)
4. 补建 `forum_like_log`
5. 修改 [tieba/src/main/resources/application.properties](/F:/zhao/chatroom/tieba/src/main/resources/application.properties)
6. 运行 `.\mvnw.cmd test`
7. 运行 `start.bat`
8. 登录页面、个人中心、发帖页、聊天室各检查一遍
