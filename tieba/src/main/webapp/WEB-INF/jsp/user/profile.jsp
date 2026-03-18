<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>个人中心 - 本地贴吧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/apple-ui.css">
    <script defer src="${pageContext.request.contextPath}/static/js/apple-ui.js"></script>
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
            <div class="alert alert-error" data-reveal>${error}</div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal>${success}</div>
        </c:if>

        <div class="profile-shell">
            <section class="panel profile-avatar-card interactive-card" data-reveal>
                <c:choose>
                    <c:when test="${not empty profile.avatarPath}">
                        <img src="${profile.avatarPath}" alt="用户头像" class="profile-avatar">
                    </c:when>
                    <c:otherwise>
                        <div class="profile-avatar profile-avatar-fallback">
                            ${empty profile.nickname ? fn:substring(account.username, 0, 1) : fn:substring(profile.nickname, 0, 1)}
                        </div>
                    </c:otherwise>
                </c:choose>

                <h2>${empty profile.nickname ? account.nickname : profile.nickname}</h2>
                <div class="profile-account-meta">
                    <div>
                        <span>登录账号</span>
                        <strong>${account.username}</strong>
                    </div>
                    <div>
                        <span>当前头像地址</span>
                        <strong>${empty profile.avatarPath ? "未上传头像" : profile.avatarPath}</strong>
                    </div>
                </div>
                <p class="upload-tip">
                    推荐上传 1:1 方形头像。支持 `jpg/png/gif/webp`，单文件不超过 2MB。
                </p>
            </section>

            <section class="panel" data-reveal>
                <form action="${pageContext.request.contextPath}/api/user/profile/update" method="post" enctype="multipart/form-data">
                    <div class="form-group">
                        <label class="form-label" for="nickname">昵称</label>
                        <input type="text" id="nickname" name="nickname" class="form-control" maxlength="30"
                               value="${empty profile.nickname ? account.nickname : profile.nickname}" required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="bio">个性签名 / 简介</label>
                        <textarea id="bio" name="bio" class="form-control" rows="6" maxlength="255"
                                  placeholder="写一句介绍自己或吧内状态的话">${profile.bio}</textarea>
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
    </main>
</body>
</html>
