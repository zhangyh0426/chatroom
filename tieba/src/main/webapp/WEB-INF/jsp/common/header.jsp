<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
    <c:when test="${not empty sessionScope.user}">
        <c:set var="interestGroupsUrl" value="${pageContext.request.contextPath}/chat/rooms#rooms-lobby" />
        <c:set var="profileUrl" value="${pageContext.request.contextPath}/user/profile" />
    </c:when>
    <c:otherwise>
        <c:url var="interestGroupsUrl" value="/auth/login">
            <c:param name="returnTo" value="/chat/rooms#rooms-lobby" />
        </c:url>
        <c:url var="profileUrl" value="/auth/login">
            <c:param name="returnTo" value="/user/profile" />
        </c:url>
    </c:otherwise>
</c:choose>
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
            <a href="${pageContext.request.contextPath}/chat/global">聊天室</a>
            <a href="${interestGroupsUrl}">兴趣群组</a>
        </nav>

        <div class="user-panel">
            <c:choose>
                <c:when test="${not empty sessionScope.user}">
                    <span>你好，<strong><c:out value="${sessionScope.user.nickname}" /></strong></span>
                    <a href="${profileUrl}" class="btn btn-sm btn-ghost">个人中心</a>
                    <a href="${pageContext.request.contextPath}/auth/logout" class="btn btn-sm btn-outline">退出</a>
                </c:when>
                <c:otherwise>
                    <a href="${profileUrl}" class="btn btn-sm btn-ghost">个人中心</a>
                    <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-sm">登录</a>
                    <a href="${pageContext.request.contextPath}/auth/register" class="btn btn-sm btn-outline">注册</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>
