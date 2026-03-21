# Fix Frontend Issues Spec

## Why
在对前端项目现状进行深度分析后，发现了几个严重问题：
1. XSS 跨站脚本攻击漏洞：用户输入未经转义即输出，可能导致恶意脚本执行。
2. JavaScript 依赖缺失：非首页未引入核心动画库（GSAP, Lenis），导致控制台报错、动画失效、甚至元素无法显示。
3. UI/UX 及性能优化：CSS 中的 `@import` 会阻塞渲染，影响加载体验。

## What Changes
- 提取并创建 `common/head.jsp`，统一管理页面 `<head>` 的 `<meta>`、`<link>`（引入 Google Fonts 替代 CSS 中的 `@import`）和核心 `<script>` 依赖（GSAP、Lenis 等）。
- 替换所有现有 JSP 页面（`index.jsp`, `board.jsp`, `thread.jsp`, `global.jsp`, `login.jsp`, `register.jsp`, `profile.jsp`）中重复的 head 元素为统一的 `head.jsp` 引入。
- 修复 `thread.jsp` 中的帖子内容 XSS 漏洞，移除不安全的 `escapeXml="false"`。
- 修复 `header.jsp` 中的昵称显示 XSS 漏洞，使用 `<c:out>` 转义。
- 移除 `apple-ui.css` 首行的 `@import url(...)`。

## Impact
- Affected specs: 前端页面渲染、脚本加载机制、安全性。
- Affected code: `src/main/webapp/WEB-INF/jsp/` 下的大部分页面以及 `static/css/apple-ui.css`。

## ADDED Requirements
### Requirement: 统一 Head 资源管理
系统前端的所有页面必须通过统一的 `head.jsp` 文件引入基础 CSS、字体和核心 JS 库。

#### Scenario: 动画依赖加载
- **WHEN** 用户访问非首页（如吧内页、帖子页）
- **THEN** 页面能够正确加载 GSAP 库并展示平滑动画，控制台无报错。

## MODIFIED Requirements
### Requirement: XSS 防御
系统前端所有展示用户输入的地方必须经过安全转义，禁止直接将不受信任的数据输出到 HTML 树中。
