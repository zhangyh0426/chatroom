# Tasks

- [x] Task 1: 调整聊天室页面信息架构
  - [x] SubTask 1.1: 在 `rooms.jsp` 中将聊天室列表区移动到创建分组区之前
  - [x] SubTask 1.2: 为聊天室列表区添加稳定锚点 `rooms-lobby`
  - [x] SubTask 1.3: 保持创建分组功能可用并放置到次级区域

- [x] Task 2: 统一前端入口落点
  - [x] SubTask 2.1: 更新头部导航“聊天室”入口为 `/chat/rooms#rooms-lobby`
  - [x] SubTask 2.2: 更新首页主按钮与中部入口为 `/chat/rooms#rooms-lobby`
  - [x] SubTask 2.3: 检查并修复其他同类“进入聊天室”链接

- [x] Task 3: 验证与回归
  - [x] SubTask 3.1: 编译验证 JSP 变更不引入构建错误
  - [x] SubTask 3.2: 手工检查关键页面入口跳转与首屏落点
  - [x] SubTask 3.3: 按 checklist 回填验收结果

# Task Dependencies

- Task 2 depends on Task 1
- Task 3 depends on Task 2
