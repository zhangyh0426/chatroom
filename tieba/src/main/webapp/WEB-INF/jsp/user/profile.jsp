<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>个人中心 - 本地贴吧</title>
    <style>
        .profile-shell {
            display: grid;
            grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
            gap: 24px;
            align-items: start;
        }
        .profile-avatar-card {
            text-align: center;
        }
        .profile-avatar {
            width: 160px;
            height: 160px;
            border-radius: 50%;
            object-fit: cover;
            display: block;
            margin: 0 auto 16px;
            border: 4px solid rgba(255, 255, 255, 0.8);
            box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
            background: linear-gradient(135deg, #dbeafe, #fde68a);
        }
        .profile-avatar-fallback {
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 56px;
            font-weight: 700;
            color: #1e293b;
        }
        .profile-account-meta {
            display: grid;
            gap: 10px;
            margin-top: 16px;
        }
        .profile-account-meta span {
            display: block;
            color: #64748b;
            font-size: 14px;
        }
        .profile-account-meta strong {
            color: #0f172a;
            font-size: 16px;
        }
        .upload-tip {
            margin-top: 12px;
            color: #64748b;
            font-size: 13px;
            line-height: 1.6;
        }
        @media (max-width: 900px) {
            .profile-shell {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body class="page-board">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <section class="panel" data-reveal>
            <p class="auth-kicker">PERSONAL CENTER</p>
            <h1 class="composer-title">编辑你的社区资料</h1>
            <p class="auth-subtitle">支持昵称、个性签名和本地头像上传，头像文件将写入外部上传目录。</p>
        </section>

        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal><c:out value="${success}" /></div>
        </c:if>

        <div class="profile-shell">
            <section class="panel profile-avatar-card interactive-card" data-reveal>
                <c:choose>
                    <c:when test="${not empty profile.avatarPath}">
                        <c:set var="profileAvatarUrl" value="${profile.avatarPath}" />
                        <c:if test="${not fn:startsWith(profileAvatarUrl, 'http://')
                                      and not fn:startsWith(profileAvatarUrl, 'https://')
                                      and not fn:startsWith(profileAvatarUrl, pageContext.request.contextPath)}">
                            <c:set var="profileAvatarUrl" value="${pageContext.request.contextPath}${profileAvatarUrl}" />
                        </c:if>
                        <img src="<c:out value='${profileAvatarUrl}' />" alt="用户头像" class="profile-avatar" onerror="this.src='${pageContext.request.contextPath}/static/img/default-avatar.svg'">
                    </c:when>
                    <c:otherwise>
                        <div class="profile-avatar profile-avatar-fallback">
                            <c:out value="${empty profile.nickname ? fn:substring(account.username, 0, 1) : fn:substring(profile.nickname, 0, 1)}" />
                        </div>
                    </c:otherwise>
                </c:choose>

                <h2><c:out value="${empty profile.nickname ? account.nickname : profile.nickname}" /></h2>
                <div class="profile-account-meta">
                    <div>
                        <span>登录账号</span>
                        <strong><c:out value="${account.username}" /></strong>
                    </div>
                    <div>
                        <span>当前头像地址</span>
                        <strong><c:out value="${empty profile.avatarPath ? '未上传头像' : profile.avatarPath}" /></strong>
                    </div>
                </div>
                <p class="upload-tip">
                    推荐上传 1:1 方形头像。支持 `jpg/png/gif/webp`，单文件不超过 2MB。
                </p>
            </section>

            <section class="panel" data-reveal>
                <form action="${pageContext.request.contextPath}/user/profile/update" method="post" enctype="multipart/form-data">
                    <div class="form-group">
                        <label class="form-label" for="nickname">昵称</label>
                        <input type="text" id="nickname" name="nickname" class="form-control" maxlength="30"
                               value="<c:out value='${empty profile.nickname ? account.nickname : profile.nickname}' />" required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="bio">个性签名 / 简介</label>
                        <textarea id="bio" name="bio" class="form-control" rows="6" maxlength="255"
                                  placeholder="写一句介绍自己或吧内状态的话"><c:out value="${profile.bio}" /></textarea>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="avatarFile">上传新头像</label>
                        <input type="file" id="avatarFile" name="avatarFile" class="form-control"
                               accept=".jpg,.jpeg,.png,.gif,.webp,image/*">
                    </div>

                    <button type="submit" class="btn">保存个人资料</button>
                </form>
            </section>
        </div>

        <section class="panel" data-reveal>
            <header class="section-head">
                <h2>我的足迹</h2>
                <span class="pill">双列表视图</span>
            </header>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:20px;">
                <article>
                    <h3 style="margin-bottom:10px;">我发的帖子</h3>
                    <c:choose>
                        <c:when test="${empty myThreads.list}">
                            <div class="empty-state">你还没有发布帖子。</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${myThreads.list}" var="t">
                                <div class="thread-meta" style="padding:8px 0;border-bottom:1px solid rgba(148,163,184,.2);display:flex;justify-content:space-between;gap:8px;">
                                    <a href="${pageContext.request.contextPath}/thread/${t.id}">
                                        <c:out value="${t.title}" />
                                    </a>
                                    <span><fmt:formatDate value="${t.createdAt}" pattern="MM-dd HH:mm"/></span>
                                </div>
                            </c:forEach>
                            <div class="thread-meta" style="margin-top:8px;display:flex;justify-content:space-between;">
                                <span>第 ${myThreads.pageNum}/${myThreads.totalPages} 页</span>
                                <span style="display:flex;gap:8px;">
                                    <c:if test="${myThreads.hasPrev()}">
                                        <a href="${pageContext.request.contextPath}/user/profile?myThreadPage=${myThreads.pageNum - 1}&myReplyPage=${myReplies.pageNum}" class="btn btn-sm btn-ghost">上一页</a>
                                    </c:if>
                                    <c:if test="${myThreads.hasNext()}">
                                        <a href="${pageContext.request.contextPath}/user/profile?myThreadPage=${myThreads.pageNum + 1}&myReplyPage=${myReplies.pageNum}" class="btn btn-sm btn-ghost">下一页</a>
                                    </c:if>
                                </span>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </article>
                <article>
                    <h3 style="margin-bottom:10px;">我回复的帖子</h3>
                    <c:choose>
                        <c:when test="${empty myReplies.list}">
                            <div class="empty-state">你还没有回复内容。</div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${myReplies.list}" var="p">
                                <div class="thread-meta" style="padding:8px 0;border-bottom:1px solid rgba(148,163,184,.2);display:grid;gap:4px;">
                                    <a href="${pageContext.request.contextPath}/thread/${p.threadId}">
                                        <c:out value="${empty p.threadTitle ? '未命名帖子' : p.threadTitle}" />
                                    </a>
                                    <span><fmt:formatDate value="${p.createdAt}" pattern="MM-dd HH:mm"/> · <c:out value="${p.content}" /></span>
                                </div>
                            </c:forEach>
                            <div class="thread-meta" style="margin-top:8px;display:flex;justify-content:space-between;">
                                <span>第 ${myReplies.pageNum}/${myReplies.totalPages} 页</span>
                                <span style="display:flex;gap:8px;">
                                    <c:if test="${myReplies.hasPrev()}">
                                        <a href="${pageContext.request.contextPath}/user/profile?myThreadPage=${myThreads.pageNum}&myReplyPage=${myReplies.pageNum - 1}" class="btn btn-sm btn-ghost">上一页</a>
                                    </c:if>
                                    <c:if test="${myReplies.hasNext()}">
                                        <a href="${pageContext.request.contextPath}/user/profile?myThreadPage=${myThreads.pageNum}&myReplyPage=${myReplies.pageNum + 1}" class="btn btn-sm btn-ghost">下一页</a>
                                    </c:if>
                                </span>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </article>
            </div>
        </section>
    </main>
</body>
</html>
