- [x] `head.jsp` 文件已正确创建并包含必要的 JS/CSS/Fonts 依赖
- [x] 所有业务 JSP 页面均已使用 `<jsp:include page="/WEB-INF/jsp/common/head.jsp" />` 替换原有静态资源引用
- [x] `thread.jsp` 中不再存在 `escapeXml="false"` 的不安全用法
- [x] `header.jsp` 中的昵称变量被 `<c:out>` 保护
- [x] `apple-ui.css` 中的 `@import` 已被移除

