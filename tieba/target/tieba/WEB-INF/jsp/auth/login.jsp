<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - 本地吧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/apple-ui.css">
    <script defer src="${pageContext.request.contextPath}/static/js/apple-ui.js"></script>
</head>
<body class="auth-page">
    <main class="auth-shell">
        <div class="auth-bg">
            <span class="auth-orb a"></span>
            <span class="auth-orb b"></span>
            <span class="auth-orb c"></span>
        </div>

        <section class="auth-card interactive-card" data-reveal>
            <p class="auth-kicker">WELCOME BACK</p>
            <h1 class="auth-title">登录你的社区身份</h1>
            <p class="auth-subtitle">继续浏览贴吧动态、参与实时聊天室和主题讨论。</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error">${error}</div>
            </c:if>
            <c:if test="${not empty msg}">
                <div class="alert alert-success">${msg}</div>
            </c:if>

            <form action="${pageContext.request.contextPath}/auth/login" method="post">
                <div class="form-group">
                    <label class="form-label" for="username">用户名</label>
                    <input type="text" id="username" name="username" class="form-control" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" required>
                </div>
                <button type="submit" class="btn btn-wide">立即登录</button>
            </form>

            <div class="auth-links">
                没有账号？<a href="${pageContext.request.contextPath}/auth/register">去注册</a>
                <br><br>
                <a href="${pageContext.request.contextPath}/">返回首页</a>
            </div>
        </section>
    </main>
</body>
</html>
