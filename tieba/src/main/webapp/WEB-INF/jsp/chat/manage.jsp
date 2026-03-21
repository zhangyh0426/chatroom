<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>创建兴趣分区与群组 - 本地贴吧</title>
</head>
<body>
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/chat/rooms">返回兴趣群组</a>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal><c:out value="${success}" /></div>
        </c:if>

        <section class="panel" data-reveal>
            <header class="section-head">
                <div>
                    <h2>创建兴趣分区与群组</h2>
                    <span class="pill">先建分区再建分组</span>
                </div>
                <div class="hero-actions">
                    <a class="btn btn-ghost" href="${pageContext.request.contextPath}/chat/rooms">返回群组列表</a>
                </div>
            </header>
            <form action="${pageContext.request.contextPath}/chat/rooms/create" method="post">
                <div class="form-group">
                    <label class="form-label" for="partitionCode">分区编码</label>
                    <input id="partitionCode"
                           type="text"
                           name="partitionCode"
                           class="form-control"
                           placeholder="示例：BOOK_ZONE"
                           required
                           maxlength="30"
                           value="<c:out value='${createPartitionCode}' />">
                </div>
                <div class="form-group">
                    <label class="form-label" for="partitionName">分区名称</label>
                    <input id="partitionName"
                           type="text"
                           name="partitionName"
                           class="form-control"
                           placeholder="示例：阅读空间"
                           required
                           maxlength="50"
                           value="<c:out value='${createPartitionName}' />">
                </div>
                <div class="form-group">
                    <label class="form-label" for="partitionSortOrder">分区排序值</label>
                    <input id="partitionSortOrder"
                           type="number"
                           name="sortOrder"
                           class="form-control"
                           min="0"
                           max="9999"
                           value="<c:out value='${createPartitionSortOrder}' />">
                </div>
                <div class="form-group">
                    <label class="form-label" for="roomCode">分组编码</label>
                    <input id="roomCode"
                           type="text"
                           name="roomCode"
                           class="form-control"
                           placeholder="示例：BOOK_CLUB"
                           required
                           maxlength="30"
                           value="<c:out value='${createRoomCode}' />">
                </div>
                <div class="form-group">
                    <label class="form-label" for="roomName">分组名称</label>
                    <input id="roomName"
                           type="text"
                           name="roomName"
                           class="form-control"
                           placeholder="示例：读书会"
                           required
                           maxlength="100"
                           value="<c:out value='${createRoomName}' />">
                </div>
                <button type="submit" class="btn">创建分组</button>
            </form>
        </section>

        <section class="panel" data-reveal>
            <header class="section-head">
                <h2>已启用兴趣分区</h2>
                <span class="pill">创建前参考</span>
            </header>
            <c:choose>
                <c:when test="${manageLoadFailed}">
                    <div class="empty-state">
                        <p><c:out value="${manageFeedback}" /></p>
                    </div>
                </c:when>
                <c:when test="${empty partitions}">
                    <div class="empty-state">
                        <p>当前还没有启用的兴趣分区，你可以直接创建第一组分区与群组。</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="board-grid">
                        <c:forEach items="${partitions}" var="partition">
                            <article class="board-card interactive-card" data-reveal>
                                <span class="board-icon">区</span>
                                <div class="board-copy">
                                    <h3><c:out value="${partition.partitionName}" /></h3>
                                    <p>分区编码：<c:out value="${partition.partitionCode}" /></p>
                                    <p>排序值：<strong><c:out value="${partition.sortOrder}" /></strong></p>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
