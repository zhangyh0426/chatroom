<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
    <c:when test="${not empty sessionScope.user}">
        <c:set var="interestGroupsEntryUrl" value="${pageContext.request.contextPath}/chat/rooms#rooms-lobby" />
    </c:when>
    <c:otherwise>
        <c:url var="interestGroupsEntryUrl" value="/auth/login">
            <c:param name="returnTo" value="/chat/rooms#rooms-lobby" />
        </c:url>
    </c:otherwise>
</c:choose>
<c:set var="postEntryUrl" value="${pageContext.request.contextPath}${postEntryPath}" />
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
                <p>用更轻盈、更沉浸的体验连接话题与人。浏览兴趣群组、进入吧内讨论，或直接进入新发帖页，整理内容后再决定是否发布。</p>
                <div class="hero-actions">
                    <a href="${postEntryUrl}" class="btn btn-accent">我要发帖</a>
                    <a href="${pageContext.request.contextPath}/chat/global" class="btn">进入聊天室</a>
                    <a href="${interestGroupsEntryUrl}" class="btn btn-ghost">浏览兴趣群组</a>
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

        <section class="panel discovery-grid-shell" data-reveal>
            <header class="section-head">
                <div>
                    <h2>首页发现流</h2>
                    <p class="section-caption">从目录导航升级为内容发现流，先逛帖子，再决定加入哪一个社区。</p>
                </div>
                <a href="${pageContext.request.contextPath}/search" class="btn btn-sm btn-ghost">去全站搜索</a>
            </header>
            <div class="discovery-grid">
                <article class="discovery-panel">
                    <header class="section-head">
                        <h3>最新发布</h3>
                        <span class="pill">Latest</span>
                    </header>
                    <c:choose>
                        <c:when test="${empty latestThreads}">
                            <div class="empty-state">暂无最新帖子</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${latestThreads}" var="thread">
                                <a href="${pageContext.request.contextPath}/thread/${thread.id}" class="discovery-thread-card">
                                    <c:if test="${not empty thread.coverImagePath}">
                                        <img src="${pageContext.request.contextPath}${thread.coverImagePath}" alt="${thread.title}" class="thread-cover">
                                    </c:if>
                                    <div class="discovery-thread-copy">
                                        <div class="thread-chip-row">
                                            <span class="pill"><c:out value="${thread.threadTypeLabel}" /></span>
                                            <span class="thread-meta"><c:out value="${thread.boardName}" /></span>
                                        </div>
                                        <h4><c:out value="${thread.title}" /></h4>
                                        <p class="thread-summary"><c:out value="${thread.content}" /></p>
                                        <div class="thread-tag-row">
                                            <c:forEach items="${thread.tagNames}" var="tagName">
                                                <span class="mini-tag">#<c:out value="${tagName}" /></span>
                                            </c:forEach>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </article>

                <article class="discovery-panel">
                    <header class="section-head">
                        <h3>热门讨论</h3>
                        <span class="pill">Hot</span>
                    </header>
                    <c:choose>
                        <c:when test="${empty hotThreads}">
                            <div class="empty-state">暂无热门帖子</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${hotThreads}" var="thread">
                                <a href="${pageContext.request.contextPath}/thread/${thread.id}" class="discovery-thread-inline">
                                    <div>
                                        <strong><c:out value="${thread.title}" /></strong>
                                        <div class="thread-tag-row">
                                            <span class="mini-tag"><c:out value="${thread.threadTypeLabel}" /></span>
                                            <c:forEach items="${thread.tagNames}" var="tagName">
                                                <span class="mini-tag">#<c:out value="${tagName}" /></span>
                                            </c:forEach>
                                        </div>
                                    </div>
                                    <span class="thread-meta">热度 ${thread.viewCount + thread.replyCount * 10}</span>
                                </a>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </article>

                <article class="discovery-panel">
                    <header class="section-head">
                        <h3>精华内容</h3>
                        <span class="pill">Essence</span>
                    </header>
                    <c:choose>
                        <c:when test="${empty essenceThreads}">
                            <div class="empty-state">暂无精华内容</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${essenceThreads}" var="thread">
                                <a href="${pageContext.request.contextPath}/thread/${thread.id}" class="discovery-thread-inline">
                                    <div>
                                        <strong><c:out value="${thread.title}" /></strong>
                                        <div class="thread-meta"><c:out value="${thread.authorName}" /> · <fmt:formatDate value="${thread.createdAt}" pattern="MM-dd HH:mm" /></div>
                                    </div>
                                    <span class="badge badge-essence">精华</span>
                                </a>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </article>

                <article class="discovery-panel">
                    <header class="section-head">
                        <h3>校园活动</h3>
                        <span class="pill">Campus</span>
                    </header>
                    <c:choose>
                        <c:when test="${empty activityThreads}">
                            <div class="empty-state">暂无活动或招募信息</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${activityThreads}" var="thread">
                                <a href="${pageContext.request.contextPath}/thread/${thread.id}" class="discovery-thread-inline">
                                    <div>
                                        <strong><c:out value="${thread.title}" /></strong>
                                        <div class="thread-meta"><c:out value="${thread.boardName}" /></div>
                                    </div>
                                    <span class="pill"><c:out value="${thread.threadTypeLabel}" /></span>
                                </a>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </article>
            </div>
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
