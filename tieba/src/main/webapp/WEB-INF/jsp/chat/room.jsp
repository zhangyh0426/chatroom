<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <jsp:include page="/WEB-INF/jsp/common/head.jsp" />
    <title><c:out value="${room.roomName}" /> - 兴趣群组</title>
</head>
<body class="page-chat">
    <jsp:include page="../common/header.jsp" />

    <main class="container">
        <div class="crumb-row" data-reveal>
            <a href="${pageContext.request.contextPath}/chat/rooms">返回群组列表</a>
        </div>
        <c:if test="${not empty error}">
            <div class="alert alert-error" data-reveal><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty success}">
            <div class="alert alert-success" data-reveal><c:out value="${success}" /></div>
        </c:if>

        <section class="panel chat-shell" data-reveal>
            <div class="chat-head">
                <h2><c:out value="${room.roomName}" /> · <c:out value="${room.roomCode}" /></h2>
                <span class="status-pill" id="ws-status"><c:out value="${joined ? '连接中...' : '未加入'}" /></span>
            </div>

            <div class="chat-head">
                <span>兴趣分区：<strong><c:out value="${room.partitionName}" /></strong></span>
                <span>房间类型：<strong><c:out value="${room.roomType}" /></strong></span>
                <span>加入状态：<strong><c:out value="${joined ? '已加入' : '未加入'}" /></strong></span>
            </div>

            <div id="chat-box" class="chat-feed">
                <c:forEach items="${history}" var="msg">
                    <c:choose>
                        <c:when test="${msg.userId == 0}">
                            <div class="chat-row system-message">
                                <div class="system-bubble"><c:out value="${msg.content}"/></div>
                            </div>
                        </c:when>
                        <c:otherwise>
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
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>

            <c:choose>
                <c:when test="${joined}">
                    <div class="chat-compose">
                        <input type="text" id="chat-input" class="form-control" placeholder="输入消息，回车发送" autocomplete="off" disabled>
                        <button type="button" class="btn btn-accent" onclick="sendMsg()" disabled>发送消息</button>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="chat-compose">
                        <input type="text" class="form-control" value="加入后才可发言" disabled>
                        <form method="post" action="${pageContext.request.contextPath}/chat/rooms/${room.roomCode}/join">
                            <button type="submit" class="btn">加入群组</button>
                        </form>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </main>

    <script>
        var joined = ${joined ? 'true' : 'false'};
        var wsScheme = location.protocol === 'https:' ? 'wss://' : 'ws://';
        var wsUrl = wsScheme + location.host + '${pageContext.request.contextPath}/ws/chat/rooms/${room.roomCode}';
        var ws;
        var currentUserId = '${sessionScope.user.id}';
        var appContextPath = '${pageContext.request.contextPath}';
        var statusLabel = document.getElementById('ws-status');
        var chatBox = document.getElementById('chat-box');
        var chatInput = document.getElementById('chat-input');
        var sendButton = document.querySelector('.chat-compose button');
        var reconnectTimer = null;
        var reconnectAttempts = 0;
        var maxReconnectAttempts = 5;
        var ConnectionState = {
            CONNECTING: 'CONNECTING',
            ONLINE: 'ONLINE',
            RECONNECTING: 'RECONNECTING',
            FAILED: 'FAILED',
            REJECTED: 'REJECTED',
            CLOSED: 'CLOSED'
        };
        var connectionState = joined ? ConnectionState.CONNECTING : ConnectionState.CLOSED;

        function setStatus(label, cls) {
            statusLabel.textContent = label;
            statusLabel.classList.remove('status-online', 'status-offline', 'status-error', 'status-connecting', 'status-reconnecting');
            if (cls) {
                statusLabel.classList.add(cls);
            }
        }

        function setInputEnabled(enabled) {
            if (chatInput) {
                chatInput.disabled = !enabled;
            }
            if (sendButton && sendButton.tagName === 'BUTTON') {
                sendButton.disabled = !enabled;
            }
        }

        function clearReconnectTimer() {
            if (reconnectTimer) {
                window.clearTimeout(reconnectTimer);
                reconnectTimer = null;
            }
        }

        function switchConnectionState(nextState, detail) {
            connectionState = nextState;
            if (nextState === ConnectionState.ONLINE) {
                setStatus('在线', 'status-online');
                setInputEnabled(true);
                return;
            }
            setInputEnabled(false);
            if (nextState === ConnectionState.CONNECTING) {
                setStatus('连接中...', 'status-connecting');
                return;
            }
            if (nextState === ConnectionState.RECONNECTING) {
                setStatus('重连中（' + reconnectAttempts + '/' + maxReconnectAttempts + '）' + (detail ? ' ' + detail : ''), 'status-reconnecting');
                return;
            }
            if (nextState === ConnectionState.REJECTED) {
                setStatus('连接被拒绝：' + (detail || '策略违规'), 'status-error');
                return;
            }
            if (nextState === ConnectionState.FAILED) {
                setStatus('连接失败：' + (detail || '请刷新页面重试'), 'status-error');
                return;
            }
            setStatus(detail || '已断开连接', 'status-offline');
        }

        function scheduleReconnect() {
            reconnectAttempts++;
            if (reconnectAttempts > maxReconnectAttempts) {
                clearReconnectTimer();
                switchConnectionState(ConnectionState.FAILED, '重连已达上限');
                return;
            }
            var delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000);
            switchConnectionState(ConnectionState.RECONNECTING, delay / 1000 + '秒后重试');
            clearReconnectTimer();
            reconnectTimer = window.setTimeout(connect, delay);
        }

        function connect() {
            if (!joined) {
                return;
            }
            if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
                return;
            }
            if (connectionState === ConnectionState.REJECTED || connectionState === ConnectionState.FAILED || connectionState === ConnectionState.CLOSED) {
                setInputEnabled(false);
            }
            switchConnectionState(reconnectAttempts > 0 ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING);
            ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                clearReconnectTimer();
                reconnectAttempts = 0;
                switchConnectionState(ConnectionState.ONLINE);
                scrollToBottom();
            };

            ws.onmessage = function (event) {
                try {
                    var msg = JSON.parse(event.data);
                    appendMessage(msg);
                } catch (e) {
                    console.error('Failed to parse message', e);
                }
            };

            ws.onclose = function (event) {
                clearReconnectTimer();
                if (event && event.code === 1008) {
                    switchConnectionState(ConnectionState.REJECTED, event.reason || '策略违规');
                    return;
                }
                if (event && event.code === 1000) {
                    switchConnectionState(ConnectionState.CLOSED, '已断开连接');
                    return;
                }
                scheduleReconnect();
            };

            ws.onerror = function () {
                if (connectionState === ConnectionState.CONNECTING || connectionState === ConnectionState.RECONNECTING) {
                    setStatus('连接异常，等待重连', 'status-offline');
                }
            };
        }

        function appendMessage(msg) {
            var isSelf = currentUserId && parseInt(currentUserId, 10) === msg.userId;
            var isSystem = msg.userId === 0;
            var row = document.createElement('div');
            
            if (isSystem) {
                row.className = 'chat-row system-message';
                row.innerHTML = '<div class="system-bubble">' + escapeHtml(msg.content || '') + '</div>';
            } else {
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
            }

            chatBox.appendChild(row);
            requestAnimationFrame(function () {
                row.classList.add('live');
            });
            scrollToBottom();
        }

        function sendMsg() {
            if (!joined) {
                return;
            }
            var val = (chatInput.value || '').trim();
            if (!val || !ws || ws.readyState !== WebSocket.OPEN || connectionState !== ConnectionState.ONLINE) {
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
            if (chatBox) {
                chatBox.scrollTop = chatBox.scrollHeight;
            }
        }

        if (chatInput) {
            chatInput.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') {
                    sendMsg();
                }
            });
        }

        scrollToBottom();
        if (joined) {
            switchConnectionState(ConnectionState.CONNECTING);
            connect();
        } else {
            switchConnectionState(ConnectionState.CLOSED, '未加入');
        }
    </script>
</body>
</html>
