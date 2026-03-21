<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>${board.name}吧 - 本地贴吧</title>
</head>
<body class="page-board">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <section class="panel board-head" data-reveal>
            <div class="board-logo">${board.name}</div>
            <div>
                <h1>${board.name}吧</h1>
                <p>${empty board.description ? "一个值得长期交流的兴趣社区。" : board.description}</p>
                <div class="board-stats">
                    <span class="stat-chip">主题 <strong data-count="${board.threadCount}">${board.threadCount}</strong></span>
                    <span class="stat-chip">回复 <strong data-count="${board.postCount}">${board.postCount}</strong></span>
                    <span class="stat-chip">吧内实时更新</span>
                </div>
            </div>
        </section>

        <section class="panel thread-stream" data-reveal>
            <header class="section-head">
                <h2>帖子流</h2>
                <c:if test="${not empty sessionScope.user}">
                    <a href="#composer" class="btn btn-sm btn-ghost">发新帖</a>
                </c:if>
            </header>
            <form action="${pageContext.request.contextPath}/board/${board.id}" method="get" class="form-group" style="display:flex;gap:10px;align-items:center;margin-bottom:16px;">
                <input type="hidden" name="size" value="${pageResult.pageSize}">
                <input type="text" name="keyword" class="form-control" placeholder="搜索标题或内容" value="${keyword}">
                <button type="submit" class="btn btn-sm">搜索</button>
                <c:if test="${not empty keyword}">
                    <a href="${pageContext.request.contextPath}/board/${board.id}" class="btn btn-sm btn-ghost">清除</a>
                </c:if>
            </form>

            <c:choose>
                <c:when test="${empty threads}">
                    <div class="empty-state">这里还没有帖子，发布第一条内容开启讨论。</div>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${threads}" var="t">
                        <article class="thread-card interactive-card" data-reveal>
                            <div class="thread-stat">
                                <span data-count="${t.replyCount}">${t.replyCount}</span>
                                <small>回复</small>
                            </div>
                            <div>
                                <a href="${pageContext.request.contextPath}/thread/${t.id}" class="thread-title">
                                    <c:if test="${t.isTop == 1}"><span class="badge badge-top">置顶</span></c:if>
                                    <c:if test="${t.isEssence == 1}"><span class="badge badge-essence">精华</span></c:if>
                                    <c:out value="${t.title}" />
                                </a>
                                <p class="thread-meta">
                                    <span><c:out value="${t.authorName}" /> · <fmt:formatDate value="${t.createdAt}" pattern="yyyy-MM-dd HH:mm"/></span>
                                    <span>
                                        最后活跃：
                                        <c:choose>
                                            <c:when test="${not empty t.lastReplyTime}">
                                                <fmt:formatDate value="${t.lastReplyTime}" pattern="yyyy-MM-dd HH:mm"/>
                                            </c:when>
                                            <c:otherwise>暂无</c:otherwise>
                                        </c:choose>
                                    </span>
                                </p>
                            </div>
                        </article>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty pageResult and pageResult.totalPages > 1}">
                <div class="thread-meta" style="margin-top:14px;display:flex;justify-content:space-between;align-items:center;">
                    <span>第 ${pageResult.pageNum} / ${pageResult.totalPages} 页，共 ${pageResult.totalCount} 条</span>
                    <span style="display:flex;gap:8px;">
                        <c:if test="${pageResult.hasPrev()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/board/${board.id}?page=${pageResult.pageNum - 1}&size=${pageResult.pageSize}&keyword=${keyword}">上一页</a>
                        </c:if>
                        <c:if test="${pageResult.hasNext()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/board/${board.id}?page=${pageResult.pageNum + 1}&size=${pageResult.pageSize}&keyword=${keyword}">下一页</a>
                        </c:if>
                    </span>
                </div>
            </c:if>
        </section>

        <section class="panel" data-reveal>
            <header class="section-head">
                <h2>热门榜</h2>
                <span class="pill">Top 10</span>
            </header>
            <c:choose>
                <c:when test="${empty hotThreads}">
                    <div class="empty-state">暂无热门帖子</div>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${hotThreads}" var="hot" varStatus="st">
                        <div class="thread-meta" style="display:flex;justify-content:space-between;gap:12px;padding:8px 0;border-bottom:1px solid rgba(148,163,184,.2);">
                            <a href="${pageContext.request.contextPath}/thread/${hot.id}">
                                #${st.index + 1} <c:out value="${hot.title}" />
                            </a>
                            <span>热度 ${hot.viewCount + hot.replyCount * 10}</span>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="panel" id="composer" data-reveal>
            <h3 class="composer-title">发表新帖子</h3>
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <form action="${pageContext.request.contextPath}/board/post/thread" method="post">
                        <input type="hidden" name="boardId" value="${board.id}">
                        <div class="form-group">
                            <label class="form-label" for="title">标题</label>
                            <input id="title" type="text" name="title" class="form-control" placeholder="一句话概括主题" required maxlength="100">
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="content">正文</label>
                            <textarea id="content" name="content" class="form-control" rows="8" placeholder="写下你的想法" required></textarea>
                        </div>
                        <button type="submit" class="btn">发布主贴</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="login-reminder">
                        需要先 <a href="${pageContext.request.contextPath}/auth/login">登录</a> 才能发帖。
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
