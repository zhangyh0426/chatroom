<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>通知中心 - 本地贴吧</title>
</head>
<body class="page-board">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/user/profile">返回个人中心</a>
        </div>

        <section class="panel" data-reveal>
            <header class="section-head">
                <div>
                    <p class="auth-kicker">NOTIFICATION CENTER</p>
                    <h1 class="composer-title">通知中心</h1>
                    <p class="section-caption">回复、点赞、提及和系统提醒都会集中沉淀到这里。</p>
                </div>
                <form action="${pageContext.request.contextPath}/user/notifications/read-all" method="post">
                    <button type="submit" class="btn btn-sm btn-ghost">全部标记已读</button>
                </form>
            </header>

            <c:choose>
                <c:when test="${empty notifications}">
                    <div class="empty-state">你还没有新的通知，继续去发帖或参与讨论吧。</div>
                </c:when>
                <c:otherwise>
                    <div class="notification-list">
                        <c:forEach items="${notifications}" var="notification">
                            <a href="${pageContext.request.contextPath}${empty notification.targetUrl ? '/user/profile' : notification.targetUrl}" class="notification-item interactive-card">
                                <div class="notification-main">
                                    <div class="thread-chip-row">
                                        <span class="pill"><c:out value="${notification.notificationType}" /></span>
                                        <c:if test="${notification.isRead == 0}">
                                            <span class="mini-tag">未读</span>
                                        </c:if>
                                    </div>
                                    <h3><c:out value="${notification.title}" /></h3>
                                    <p class="thread-summary"><c:out value="${notification.content}" /></p>
                                    <div class="thread-meta">
                                        <span><c:out value="${empty notification.actorNickname ? '系统' : notification.actorNickname}" /></span>
                                        <span><fmt:formatDate value="${notification.createdAt}" pattern="MM-dd HH:mm" /></span>
                                    </div>
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <c:if test="${pageResult.totalPages > 1}">
                <div class="thread-meta" style="margin-top:14px;display:flex;justify-content:space-between;align-items:center;">
                    <span>第 ${pageResult.pageNum} / ${pageResult.totalPages} 页，共 ${pageResult.totalCount} 条</span>
                    <span style="display:flex;gap:8px;">
                        <c:if test="${pageResult.hasPrev()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/user/notifications?page=${pageResult.pageNum - 1}&size=${pageResult.pageSize}">上一页</a>
                        </c:if>
                        <c:if test="${pageResult.hasNext()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/user/notifications?page=${pageResult.pageNum + 1}&size=${pageResult.pageSize}">下一页</a>
                        </c:if>
                    </span>
                </div>
            </c:if>
        </section>
    </main>
</body>
</html>
