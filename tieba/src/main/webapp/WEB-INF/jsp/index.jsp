<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>首页 - 本地贴吧</title>
</head>
<body class="page-home">
    <jsp:include page="common/header.jsp" />

    <main class="container">
        <section class="hero-shell" data-reveal>
            <div class="hero-copy">
                <p class="hero-kicker">LOCAL COMMUNITY HUB</p>
                <h1>把兴趣变成持续发声的舞台</h1>
                <p>用更轻盈、更沉浸的体验连接话题与人。浏览分区、进入吧内讨论，或直接进入聊天室，实时交流。</p>
                <div class="hero-actions">
                    <a href="${pageContext.request.contextPath}/chat/global" class="btn">进入聊天室</a>
                    <a href="#board-zone" class="btn btn-ghost">浏览兴趣分区</a>
                </div>
            </div>

            <div class="hero-visual">
                <span class="hero-orb orb-a"></span>
                <span class="hero-orb orb-b"></span>
                <span class="hero-orb orb-c"></span>

                <div class="metric-stack">
                    <div class="metric-card interactive-card" data-reveal data-delay="80">
                        <div class="metric-label">社区状态</div>
                        <div class="metric-value">在线开放</div>
                    </div>
                    <div class="metric-card interactive-card" data-reveal data-delay="140">
                        <div class="metric-label">交流方式</div>
                        <div class="metric-value">帖子 + 实时群聊</div>
                    </div>
                    <div class="metric-card interactive-card" data-reveal data-delay="200">
                        <div class="metric-label">体验基调</div>
                        <div class="metric-value">清晰、流畅、专注</div>
                    </div>
                </div>
            </div>
        </section>

        <section class="panel chat-gateway interactive-card" data-reveal>
            <div>
                <h3>聊天室与兴趣群组</h3>
                <p>进入后可选择全站大厅或兴趣群组。加入群组后即可实时交流，消息按群组隔离。</p>
            </div>
            <a href="${pageContext.request.contextPath}/chat/global" class="btn btn-accent">进入聊天室</a>
        </section>

        <section id="board-zone" data-reveal>
            <c:choose>
                <c:when test="${empty indexData}">
                    <div class="panel empty-state">当前还没有可展示的分区，先去创建第一个吧。</div>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${indexData}" var="entry">
                        <article class="panel category-panel" data-reveal>
                            <header class="section-head">
                                <h2>${entry.key.name}</h2>
                                <span class="pill">持续更新中</span>
                            </header>

                            <div class="board-grid">
                                <c:forEach items="${entry.value}" var="board">
                                    <a href="${pageContext.request.contextPath}/board/${board.id}" class="board-card interactive-card" data-reveal>
                                        <span class="board-icon">吧</span>
                                        <div class="board-copy">
                                            <h3>${board.name}吧</h3>
                                            <p>${board.description == null ? "全心全意为吧友服务" : board.description}</p>
                                        </div>
                                        <div class="board-meta">
                                            <span>主题 <strong data-count="${board.threadCount}">${board.threadCount}</strong></span>
                                            <span>帖子 <strong data-count="${board.postCount}">${board.postCount}</strong></span>
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
