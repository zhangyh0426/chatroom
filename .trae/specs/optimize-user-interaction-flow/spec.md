# Optimize User Interaction Flow Spec

## Why
作为产品经理和资深研发，经过对现有系统（本地贴吧）使用逻辑的详细研判，发现以下核心体验不足：
1. **回帖体验中断（Lost Context）**：用户在帖子（Thread）中回复或楼中楼回复后，系统简单重定向回 `/thread/{threadId}` 顶部。用户无法立刻看到自己的回复，需要手动滚动寻找，体验割裂。
2. **状态拦截生硬**：如未登录时尝试发帖或回复，若通过接口层触发异常，`GlobalExceptionHandler` 会将其直接重定向至登录页，但并未如 `LoginInterceptor` 那样携带 `returnTo` 记录当前所在页面。用户登录后会丢失原本的浏览上下文，无法顺畅回到之前正准备回复的帖子或吧内。
3. **未登录引导缺失闭环**：`board.jsp`（发帖区）和 `thread.jsp`（回帖区）的未登录引导文案仅仅给出了一个纯粹的 `/auth/login` 链接，导致用户点击登录后同样无法回到发帖页面。

## What Changes
- 修改 `ForumService` 及其实现，使得 `createPost` 和 `createReply` 能够返回所创建实体的 ID。
- 修改 `ThreadController` 中的 `reply` 和 `subreply` 方法，在重定向 URL 后拼接锚点（例如 `#post-{postId}`），从而实现回帖后的精准定位。
- 修改 `thread.jsp`，在对应的 HTML 元素上增加 `id="post-${post.id}"` 以支持锚点跳转。
- 优化 `GlobalExceptionHandler.java` 中处理“请先登录”逻辑的代码，提取 `Referer` 构建带有 `returnTo` 的重定向链接。
- 优化 `board.jsp` 和 `thread.jsp`，在未登录提示区域的 `/auth/login` 链接中，带上当前的 `returnTo` 变量（如 `?returnTo=/board/${board.id}` 或 `?returnTo=/thread/${thread.id}`）。

## Impact
- Affected specs: 帖子回复流、全局异常拦截与鉴权流。
- Affected code:
  - `com.chatroom.tieba.service.ForumService`
  - `com.chatroom.tieba.service.impl.ForumServiceImpl`
  - `com.chatroom.tieba.controller.ThreadController`
  - `com.chatroom.tieba.controller.GlobalExceptionHandler`
  - `src/main/webapp/WEB-INF/jsp/thread/thread.jsp`
  - `src/main/webapp/WEB-INF/jsp/board/board.jsp`

## ADDED Requirements
### Requirement: Reply Anchor Redirection
- **WHEN** 用户提交回帖或楼中楼回复成功后
- **THEN** 系统重定向回帖子详情页时，应当跳转至该回复对应的页面锚点（如 `#post-123`），避免用户迷失。

## MODIFIED Requirements
### Requirement: Unauthenticated Redirect Loop
- **WHEN** 用户在未登录状态下点击页面底部的登录链接，或触发拦截异常时
- **THEN** 应当带上当前页面的 URL 作为 `returnTo` 参数；用户完成登录后，系统能自动将用户带回刚才的帖子或吧内。