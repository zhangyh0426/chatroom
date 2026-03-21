<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>兴趣群组 - 本地贴吧</title>
</head>
<body>
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty info}">
            <div class="alert alert-info" data-reveal><c:out value="${info}" /></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal><c:out value="${success}" /></div>
        </c:if>

        <section id="rooms-lobby" class="panel" data-reveal>
            <header class="section-head">
                <div>
                    <h2>兴趣群组</h2>
                    <span class="pill">分区视图</span>
                </div>
                <div class="hero-actions">
                    <a class="btn btn-accent" href="${pageContext.request.contextPath}/chat/global">进入全站大厅</a>
                    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/chat/rooms/manage">创建兴趣分组</a>
                </div>
            </header>
            <c:choose>
                <c:when test="${roomsLoadFailed}">
                    <div class="empty-state">
                        <p><c:out value="${roomsFeedback}" /></p>
                        <div class="hero-actions">
                            <a class="btn" href="${pageContext.request.contextPath}/chat/rooms">重试加载</a>
                            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/chat/rooms/manage">创建兴趣分组</a>
                        </div>
                    </div>
                </c:when>
                <c:when test="${empty rooms}">
                    <div class="empty-state">
                        <p>当前暂无可进入的兴趣群组，请稍后重试。</p>
                        <div class="hero-actions">
                            <a class="btn" href="${pageContext.request.contextPath}/chat/rooms">刷新列表</a>
                            <a class="btn btn-ghost" href="${pageContext.request.contextPath}/chat/rooms/manage">创建兴趣分组</a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${partitionedRooms}" var="entry">
                        <article class="panel category-panel" data-reveal>
                            <header class="section-head">
                                <h3><c:out value="${entry.key.partitionName}" /></h3>
                                <span class="pill"><c:out value="${entry.key.partitionCode}" /></span>
                            </header>
                            <div class="board-grid">
                                <c:forEach items="${entry.value}" var="room">
                                    <a class="board-card interactive-card"
                                       data-reveal
                                       href="${pageContext.request.contextPath}/chat/rooms/${room.roomCode}">
                                        <span class="board-icon">群</span>
                                        <div class="board-copy">
                                            <h3><c:out value="${room.roomName}" /></h3>
                                            <p>房间编码：<c:out value="${room.roomCode}" /></p>
                                            <p>成员数：<strong><c:out value="${room.memberCount}" /></strong></p>
                                        </div>
                                        <div class="board-meta">
                                            <span>状态 <strong><c:out value="${room.status}" /></strong></span>
                                            <span>已加入 <strong><c:out value="${room.joined ? '是' : '否'}" /></strong></span>
                                        </div>
                                        <div class="hero-actions">
                                            <span class="board-cta ${room.joined ? 'is-joined' : ''}">
                                                <c:out value="${room.joined ? '进入群组' : '查看群组'}" />
                                            </span>
                                        </div>
                                    </a>
                                </c:forEach>
                            </div>
                        </article>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
