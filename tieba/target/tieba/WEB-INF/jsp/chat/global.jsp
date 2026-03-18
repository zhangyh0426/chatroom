<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>全站公共聊天室 - 本地贴吧</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/apple-ui.css">
    <script defer src="${pageContext.request.contextPath}/static/js/apple-ui.js"></script>
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
                            <img src="${pageContext.request.contextPath}/static/img/default-avatar.png" alt="avatar" onerror="this.src='data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='">
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
        var wsUrl = 'ws://' + location.host + '${pageContext.request.contextPath}/ws/chat/global';
        var ws;
        var currentUserId = '${sessionScope.user.id}';
        var statusLabel = document.getElementById('ws-status');
        var chatBox = document.getElementById('chat-box');
        var chatInput = document.getElementById('chat-input');

        function setStatus(label, cls) {
            statusLabel.textContent = label;
            statusLabel.classList.remove('status-online', 'status-offline', 'status-error');
            if (cls) {
                statusLabel.classList.add(cls);
            }
        }

        function connect() {
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

            ws.onclose = function () {
                setStatus('已断开，尝试重连中', 'status-offline');
                window.setTimeout(connect, 3000);
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

            row.innerHTML =
                '<div class="chat-avatar"><img src="${pageContext.request.contextPath}/static/img/default-avatar.png" alt="avatar"></div>' +
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
