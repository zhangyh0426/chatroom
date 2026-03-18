<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>公共聊天室</title>
</head>
<body>
    <h1>全站公共聊天室</h1>
    <div id="messages" style="height: 240px; overflow-y: auto; border: 1px solid #ccc; padding: 8px;"></div>
    <div style="margin-top: 12px;">
        <input id="messageInput" type="text" style="width: 320px;" placeholder="输入消息" />
        <button id="sendButton" type="button">发送</button>
    </div>

    <script>
        const messages = document.getElementById("messages");
        const input = document.getElementById("messageInput");
        const button = document.getElementById("sendButton");
        const protocol = window.location.protocol === "https:" ? "wss://" : "ws://";
        const socket = new WebSocket(protocol + window.location.host + "<%= request.getContextPath() %>/ws/chat/global");

        socket.onmessage = function (event) {
            const item = document.createElement("div");
            item.textContent = event.data;
            messages.appendChild(item);
            messages.scrollTop = messages.scrollHeight;
        };

        button.addEventListener("click", function () {
            if (input.value.trim() !== "") {
                socket.send(input.value.trim());
                input.value = "";
            }
        });
    </script>
</body>
</html>
