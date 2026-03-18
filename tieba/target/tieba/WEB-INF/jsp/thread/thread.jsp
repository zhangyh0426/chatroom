<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${thread.title} - 本地贴吧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/apple-ui.css">
    <script defer src="${pageContext.request.contextPath}/static/js/apple-ui.js"></script>
    <script>
        function toggleSubReply(postId, replyUserId, replyUserName) {
            var formDiv = document.getElementById('sub-form-' + postId);
            if (!formDiv) {
                return;
            }

            var willOpen = !formDiv.classList.contains('is-open');
            formDiv.classList.toggle('is-open', willOpen);

            var inputUid = document.getElementById('reply-to-uid-' + postId);
            var textarea = document.getElementById('reply-content-' + postId);

            if (willOpen && inputUid && textarea) {
                if (replyUserId) {
                    inputUid.value = replyUserId;
                    textarea.placeholder = '回复 ' + replyUserName + '：';
                } else {
                    inputUid.value = '';
                    textarea.placeholder = '我也说一句...';
                }
                textarea.focus();
            }
        }
    </script>
</head>
<body class="page-thread">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/board/${boardId}">返回吧内列表</a>
        </div>

        <section class="panel thread-hero" data-reveal>
            <h1><c:out value="${thread.title}" /></h1>
            <div class="thread-head-meta">
                浏览 <strong data-count="${thread.viewCount}">${thread.viewCount}</strong>
                · 回复 <strong data-count="${thread.replyCount}">${thread.replyCount}</strong>
                · 发布时间 <fmt:formatDate value="${thread.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
            </div>
        </section>

        <c:forEach items="${posts}" var="post">
            <article class="post-layout" data-reveal>
                <aside class="post-author interactive-card">
                    <div class="avatar-ring">
                        <img src="${pageContext.request.contextPath}/static/img/default-avatar.png" alt="avatar" onerror="this.src='data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='">
                    </div>
                    <div class="post-nickname"><c:out value="${post.authorName}"/></div>
                </aside>

                <section class="post-body interactive-card">
                    <div class="post-content">
                        <c:out value="${post.content}" escapeXml="false"/>
                    </div>

                    <div class="post-foot">
                        <span>${post.floorNo}楼 · <fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm"/></span>
                        <button type="button" class="reply-link" onclick="toggleSubReply('${post.id}', '', '')">回复</button>
                    </div>

                    <div class="reply-cloud">
                        <c:choose>
                            <c:when test="${empty post.replies}">
                                <div class="reply-empty">还没有楼中楼回复。</div>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${post.replies}" var="reply">
                                    <div class="reply-item">
                                        <strong><c:out value="${reply.authorName}"/></strong>
                                        <c:if test="${not empty reply.replyToUserName}">
                                            回复 <strong><c:out value="${reply.replyToUserName}"/></strong>
                                        </c:if>
                                        ：<c:out value="${reply.content}"/>
                                        <span class="thread-head-meta">（<fmt:formatDate value="${reply.createdAt}" pattern="HH:mm"/>）</span>
                                        <button type="button" class="reply-link" onclick="toggleSubReply('${post.id}', '${reply.userId}', '${reply.authorName}')">继续回复</button>
                                    </div>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>

                        <div class="sub-form" id="sub-form-${post.id}">
                            <c:choose>
                                <c:when test="${not empty sessionScope.user}">
                                    <form action="${pageContext.request.contextPath}/thread/subreply" method="post">
                                        <input type="hidden" name="threadId" value="${thread.id}">
                                        <input type="hidden" name="postId" value="${post.id}">
                                        <input type="hidden" id="reply-to-uid-${post.id}" name="replyToUserId" value="">
                                        <textarea id="reply-content-${post.id}" name="content" class="form-control" rows="3" placeholder="我也说一句..." required></textarea>
                                        <button type="submit" class="btn btn-sm">发表回复</button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <div class="reply-empty">请先登录再参与回复。</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </section>
            </article>
        </c:forEach>

        <section class="panel" data-reveal>
            <h3 class="composer-title">发表本帖回复</h3>
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <form action="${pageContext.request.contextPath}/thread/reply" method="post">
                        <input type="hidden" name="threadId" value="${thread.id}">
                        <div class="form-group">
                            <textarea name="content" class="form-control" rows="6" placeholder="写下你的回复..." required></textarea>
                        </div>
                        <button type="submit" class="btn">发送回复</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="login-reminder">
                        需要先 <a href="${pageContext.request.contextPath}/auth/login">登录</a> 才能回复。
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
