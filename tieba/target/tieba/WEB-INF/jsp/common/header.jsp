<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="site-header" data-reveal>
    <div class="header-inner">
        <a class="brand" href="${pageContext.request.contextPath}/">
            <span class="brand-dot"></span>
            <span class="brand-copy">
                <strong>LOCAL TIEBA</strong>
                <em>Fluid Community Experience</em>
            </span>
        </a>

        <nav class="nav-links">
            <a href="${pageContext.request.contextPath}/">发现</a>
            <a href="${pageContext.request.contextPath}/chat/global">公共聊天室</a>
        </nav>

        <div class="user-panel">
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <span>你好，<strong>${sessionScope.user.nickname}</strong></span>
                    <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-sm btn-outline">退出</a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-sm">登录</a>
                    <a href="${pageContext.request.contextPath}/auth/register" class="btn btn-sm btn-ghost">注册</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>
