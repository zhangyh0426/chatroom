<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>用户注册 - 本地吧</title>
</head>
<body class="auth-page">
    <main class="auth-shell">
        <div class="auth-bg">
            <span class="auth-orb a"></span>
            <span class="auth-orb b"></span>
            <span class="auth-orb c"></span>
        </div>

        <section class="auth-card interactive-card" data-reveal>
            <p class="auth-kicker">CREATE ACCOUNT</p>
            <h1 class="auth-title">注册并加入讨论</h1>
            <p class="auth-subtitle">创建账号后可发帖、回帖，并进入聊天室与兴趣群组。</p>

            <c:if test="${not empty error}">
                <div class="alert alert-error"><c:out value="${error}" /></div>
            </c:if>

            <form action="${pageContext.request.contextPath}/auth/register" method="post">
                <div class="form-group">
                    <label class="form-label" for="username">用户名（用于登录）</label>
                    <input type="text" id="username" name="username" class="form-control" placeholder="英文或数字" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="nickname">昵称（用于展示）</label>
                    <input type="text" id="nickname" name="nickname" class="form-control" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="password">密码</label>
                    <input type="password" id="password" name="password" class="form-control" required>
                </div>
                <button type="submit" class="btn btn-wide btn-accent">创建账号</button>
            </form>

            <div class="auth-links">
                已有账号？<a href="${pageContext.request.contextPath}/auth/login">立即登录</a>
                <br><br>
                <a href="${pageContext.request.contextPath}/">返回首页</a>
            </div>
        </section>
    </main>
</body>
</html>
