<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>全站搜索 - 本地贴吧</title>
</head>
<body class="page-board">
    <jsp:include page="common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <section class="panel" data-reveal>
            <header class="section-head">
                <div>
                    <h1 class="composer-title">全站搜索</h1>
                    <p class="section-caption">支持按关键字、版块、帖子类型和标签进行检索。</p>
                </div>
                <span class="pill">Discovery</span>
            </header>

            <form action="${pageContext.request.contextPath}/search" method="get" class="search-filter-grid">
                <input type="text" name="keyword" class="form-control" placeholder="搜索标题、正文或话题" value="${keyword}">
                <select name="boardId" class="form-control">
                    <option value="">全部版块</option>
                    <c:forEach items="${boardOptions}" var="boardOption">
                        <option value="${boardOption.id}" <c:if test="${selectedBoardId == boardOption.id}">selected</c:if>>
                            <c:out value="${boardOption.name}" />
                        </option>
                    </c:forEach>
                </select>
                <select name="threadType" class="form-control">
                    <option value="">全部类型</option>
                    <c:forEach items="${threadTypeOptions}" var="typeOption">
                        <option value="${typeOption.code}" <c:if test="${threadType eq typeOption.code}">selected</c:if>>
                            <c:out value="${typeOption.label}" />
                        </option>
                    </c:forEach>
                </select>
                <input type="text" name="tag" class="form-control" placeholder="标签，如：春招" value="${tag}">
                <button type="submit" class="btn">开始搜索</button>
                <a href="${pageContext.request.contextPath}/search" class="btn btn-ghost">清空筛选</a>
            </form>
        </section>

        <section class="panel" data-reveal>
            <header class="section-head">
                <h2>搜索结果</h2>
                <span class="pill">共 ${pageResult.totalCount} 条</span>
            </header>
            <c:choose>
                <c:when test="${empty threads}">
                    <div class="empty-state">没有找到匹配结果，换个关键字或放宽筛选条件试试。</div>
                </c:when>
                <c:otherwise>
                    <div class="search-result-grid">
                        <c:forEach items="${threads}" var="thread">
                            <a href="${pageContext.request.contextPath}/thread/${thread.id}" class="search-result-card interactive-card">
                                <c:if test="${not empty thread.coverImagePath}">
                                    <img src="${pageContext.request.contextPath}${thread.coverImagePath}" alt="${thread.title}" class="thread-cover">
                                </c:if>
                                <div class="thread-chip-row">
                                    <span class="pill"><c:out value="${thread.threadTypeLabel}" /></span>
                                    <span class="thread-meta"><c:out value="${thread.boardName}" /></span>
                                </div>
                                <h3><c:out value="${thread.title}" /></h3>
                                <p class="thread-summary"><c:out value="${thread.content}" /></p>
                                <div class="thread-tag-row">
                                    <c:forEach items="${thread.tagNames}" var="tagName">
                                        <span class="mini-tag">#<c:out value="${tagName}" /></span>
                                    </c:forEach>
                                </div>
                                <div class="thread-meta">
                                    <span><c:out value="${thread.authorName}" /></span>
                                    <span><fmt:formatDate value="${thread.createdAt}" pattern="MM-dd HH:mm" /></span>
                                </div>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>

            <c:if test="${pageResult.totalPages > 1}">
                <div class="thread-meta" style="margin-top:14px;display:flex;justify-content:space-between;align-items:center;">
                    <span>第 ${pageResult.pageNum} / ${pageResult.totalPages} 页，共 ${pageResult.totalCount} 条</span>
                    <span style="display:flex;gap:8px;">
                        <c:if test="${pageResult.hasPrev()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/search?page=${pageResult.pageNum - 1}&size=${pageResult.pageSize}&keyword=${keyword}&boardId=${selectedBoardId}&threadType=${threadType}&tag=${tag}">上一页</a>
                        </c:if>
                        <c:if test="${pageResult.hasNext()}">
                            <a class="btn btn-sm btn-ghost" href="${pageContext.request.contextPath}/search?page=${pageResult.pageNum + 1}&size=${pageResult.pageSize}&keyword=${keyword}&boardId=${selectedBoardId}&threadType=${threadType}&tag=${tag}">下一页</a>
                        </c:if>
                    </span>
                </div>
            </c:if>
        </section>
    </main>
</body>
</html>
