<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>兴趣群组 - 本地贴吧</title>
</head>
<body>
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty info}">
            <div class="alert alert-info" data-reveal><c:out value="${info}" /></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal><c:out value="${success}" /></div>
        </c:if>

        <section id="rooms-lobby" class="panel" data-reveal>
            <header class="section-head">
                <div>
                    <h2>兴趣群组</h2>
                    <span class="pill">分区视图</span>
                </div>
                <div class="hero-actions">
                    <a class="btn btn-accent" href="${pageContext.request.contextPath}/chat/global">进入全站大厅</a>
                    <c:if test="${not roomsLoadFailed}">
                        <button type="button"
                                class="btn btn-ghost"
                                data-room-create-toggle
                                data-target="rooms-create">
                            快速创建群组
                        </button>
                    </c:if>
                </div>
            </header>
            <c:if test="${not roomsLoadFailed}">
                <section id="rooms-create"
                         class="quick-create-card ${showCreatePanel ? 'is-open' : ''}"
                         data-room-create-root
                         data-open="${showCreatePanel}">
                    <header class="quick-create-head">
                        <div>
                            <h3>快速创建群组</h3>
                            <p class="section-caption">只需要群组名称和归属分区，内部编码由系统自动处理。</p>
                        </div>
                    </header>
                    <form action="${pageContext.request.contextPath}/chat/rooms/create" method="post" class="quick-create-form">
                        <input type="hidden"
                               name="partitionMode"
                               value="<c:out value='${createPartitionMode}' />"
                               data-partition-mode>
                        <c:choose>
                            <c:when test="${not empty partitions}">
                                <div class="form-group">
                                    <label class="form-label" for="existingPartitionCode">归属分区</label>
                                    <select id="existingPartitionCode"
                                            name="existingPartitionCode"
                                            class="form-control"
                                            data-partition-picker>
                                        <c:forEach items="${partitions}" var="partition">
                                            <option value="${partition.partitionCode}"
                                                <c:if test="${createPartitionMode ne 'new' and createExistingPartitionCode eq partition.partitionCode}">selected</c:if>>
                                                <c:out value="${partition.partitionName}" />
                                            </option>
                                        </c:forEach>
                                        <option value="__NEW__" <c:if test="${createPartitionMode eq 'new'}">selected</c:if>>
                                            + 新建分区
                                        </option>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="existingPartitionCode" value="">
                                <p class="section-caption is-emphasis">当前还没有启用分区，这次会先创建一个新分区。</p>
                            </c:otherwise>
                        </c:choose>
                        <div class="form-group partition-name-group ${createPartitionMode eq 'new' or empty partitions ? '' : 'is-hidden'}"
                             data-new-partition-group>
                            <label class="form-label" for="newPartitionName">新分区名称</label>
                            <input id="newPartitionName"
                                   type="text"
                                   name="newPartitionName"
                                   class="form-control"
                                   maxlength="50"
                                   placeholder="示例：阅读空间"
                                   value="<c:out value='${createNewPartitionName}' />"
                                   data-new-partition-input>
                        </div>
                        <div class="form-group">
                            <label class="form-label" for="roomName">群组名称</label>
                            <input id="roomName"
                                   type="text"
                                   name="roomName"
                                   class="form-control"
                                   maxlength="100"
                                   placeholder="示例：读书会"
                                   required
                                   value="<c:out value='${createRoomName}' />">
                        </div>
                        <div class="hero-actions quick-create-actions">
                            <button type="submit" class="btn">创建群组</button>
                            <button type="button"
                                    class="btn btn-ghost"
                                    data-room-create-toggle
                                    data-target="rooms-create">
                                收起
                            </button>
                        </div>
                    </form>
                </section>
            </c:if>
            <c:choose>
                <c:when test="${roomsLoadFailed}">
                    <div class="empty-state">
                        <p><c:out value="${roomsFeedback}" /></p>
                        <div class="hero-actions">
                            <a class="btn" href="${pageContext.request.contextPath}/chat/rooms">重试加载</a>
                        </div>
                    </div>
                </c:when>
                <c:when test="${empty rooms}">
                    <div class="empty-state">
                        <p>当前还没有兴趣群组，可以直接使用上方快速创建入口。</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${partitionedRooms}" var="entry">
                        <article class="panel category-panel" data-reveal>
                            <header class="section-head">
                                <h3><c:out value="${entry.key.partitionName}" /></h3>
                                <span class="pill"><c:out value="${entry.key.partitionCode}" /></span>
                            </header>
                            <div class="board-grid">
                                <c:forEach items="${entry.value}" var="room">
                                    <a class="board-card interactive-card"
                                       data-reveal
                                       href="${pageContext.request.contextPath}/chat/rooms/${room.roomCode}">
                                        <span class="board-icon">群</span>
                                        <div class="board-copy">
                                            <h3><c:out value="${room.roomName}" /></h3>
                                            <p>成员数：<strong><c:out value="${room.memberCount}" /></strong></p>
                                        </div>
                                        <div class="board-meta">
                                            <span>已加入 <strong><c:out value="${room.joined ? '是' : '否'}" /></strong></span>
                                        </div>
                                        <div class="hero-actions">
                                            <span class="board-cta ${room.joined ? 'is-joined' : ''}">
                                                <c:out value="${room.joined ? '进入群组' : '查看群组'}" />
                                            </span>
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
