<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title>全站公共聊天室 - 本地贴吧</title>
</head>
<body class="page-chat">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/">返回首页</a>
        </div>

        <section class="panel chat-shell" data-reveal>
            <div class="chat-head">
                <h2>GLOBAL CHAT · 全站公共聊天室</h2>
                <span class="status-pill" id="ws-status">连接中...</span>
            </div>

            <div id="chat-box" class="chat-feed">
                <c:forEach items="${history}" var="msg">
                    <div class="chat-row ${sessionScope.user.id == msg.userId ? 'self' : ''}">
                        <div class="chat-avatar">
                            <c:choose>
                                <c:when test="${not empty msg.avatar}">
                                    <c:set var="historyAvatarUrl" value="${msg.avatar}" />
                                    <c:if test="${not (fn:startsWith(historyAvatarUrl, 'http://')
                                                       or fn:startsWith(historyAvatarUrl, 'https://')
                                                       or fn:startsWith(historyAvatarUrl, pageContext.request.contextPath))}">
                                        <c:set var="historyAvatarUrl" value="${pageContext.request.contextPath}${historyAvatarUrl}" />
                                    </c:if>
                                    <img src="${historyAvatarUrl}" alt="avatar" onerror="this.src='${pageContext.request.contextPath}/static/img/default-avatar.svg'">
                                </c:when>
                                <c:otherwise>
                                    <img src="${pageContext.request.contextPath}/static/img/default-avatar.svg" alt="avatar">
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div>
                            <div class="chat-meta">
                                <strong><c:out value="${msg.nickname}"/></strong>
                                <span><fmt:formatDate value="${msg.createdAt}" pattern="MM-dd HH:mm:ss"/></span>
                            </div>
                            <div class="chat-bubble"><c:out value="${msg.content}"/></div>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <div class="chat-compose">
                <input type="text" id="chat-input" class="form-control" placeholder="输入消息，回车发送" autocomplete="off">
                <button type="button" class="btn btn-accent" onclick="sendMsg()">发送</button>
            </div>
        </section>
    </main>

    <script>
        var wsScheme = location.protocol === 'https:' ? 'wss://' : 'ws://';
        var wsUrl = wsScheme + location.host + '${pageContext.request.contextPath}/ws/chat/rooms/GLOBAL';
        var ws;
        var currentUserId = '${sessionScope.user.id}';
        var appContextPath = '${pageContext.request.contextPath}';
        var statusLabel = document.getElementById('ws-status');
        var chatBox = document.getElementById('chat-box');
        var chatInput = document.getElementById('chat-input');
        var reconnectTimer = null;
        var canReconnect = true;

        function setStatus(label, cls) {
            statusLabel.textContent = label;
            statusLabel.classList.remove('status-online', 'status-offline', 'status-error');
            if (cls) {
                statusLabel.classList.add(cls);
            }
        }

        function connect() {
            if (!canReconnect) {
                return;
            }
            ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                setStatus('在线', 'status-online');
                scrollToBottom();
            };

            ws.onmessage = function (event) {
                try {
                    var msg = JSON.parse(event.data);
                    appendMessage(msg);
                } catch (e) {
                    console.error(e);
                }
            };

            ws.onclose = function (event) {
                if (event && event.code === 1008) {
                    canReconnect = false;
                    setStatus('连接被拒绝，请重新登录', 'status-error');
                    return;
                }
                setStatus('已断开，尝试重连中', 'status-offline');
                if (reconnectTimer) {
                    window.clearTimeout(reconnectTimer);
                }
                reconnectTimer = window.setTimeout(connect, 3000);
            };

            ws.onerror = function () {
                setStatus('连接异常', 'status-error');
            };
        }

        function appendMessage(msg) {
            var isSelf = currentUserId && parseInt(currentUserId, 10) === msg.userId;
            var row = document.createElement('div');
            row.className = 'chat-row' + (isSelf ? ' self' : '');

            var date = new Date(msg.createdAt);
            if (isNaN(date.getTime())) {
                date = new Date();
            }

            var ts = formatTime(date);
            var avatarUrl = resolveAvatarUrl(msg.avatar);

            row.innerHTML =
                '<div class="chat-avatar"><img src="' + escapeHtml(avatarUrl) + '" alt="avatar" onerror="this.src=\'' + appContextPath + '/static/img/default-avatar.svg\'"></div>' +
                '<div>' +
                    '<div class="chat-meta"><strong>' + escapeHtml(msg.nickname || '游客') + '</strong><span>' + ts + '</span></div>' +
                    '<div class="chat-bubble">' + escapeHtml(msg.content || '') + '</div>' +
                '</div>';

            chatBox.appendChild(row);
            requestAnimationFrame(function () {
                row.classList.add('live');
            });
            scrollToBottom();
        }

        function sendMsg() {
            var val = (chatInput.value || '').trim();
            if (!val || !ws || ws.readyState !== WebSocket.OPEN) {
                return;
            }
            ws.send(val);
            chatInput.value = '';
        }

        function formatTime(date) {
            var mm = String(date.getMonth() + 1).padStart(2, '0');
            var dd = String(date.getDate()).padStart(2, '0');
            var hh = String(date.getHours()).padStart(2, '0');
            var mi = String(date.getMinutes()).padStart(2, '0');
            var ss = String(date.getSeconds()).padStart(2, '0');
            return mm + '-' + dd + ' ' + hh + ':' + mi + ':' + ss;
        }

        function escapeHtml(value) {
            return String(value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function resolveAvatarUrl(avatar) {
            if (!avatar) {
                return appContextPath + '/static/img/default-avatar.svg';
            }
            if (/^https?:\/\//i.test(avatar)) {
                return avatar;
            }
            if (appContextPath && avatar.indexOf(appContextPath) === 0) {
                return avatar;
            }
            if (avatar.charAt(0) === '/') {
                return appContextPath + avatar;
            }
            return appContextPath + '/' + avatar;
        }

        function scrollToBottom() {
            chatBox.scrollTop = chatBox.scrollHeight;
        }

        chatInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                sendMsg();
            }
        });

        connect();
    </script>
</body>
</html>
