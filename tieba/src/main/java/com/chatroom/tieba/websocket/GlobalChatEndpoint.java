package com.chatroom.tieba.websocket;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumChatMessage;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.service.ChatRoomService;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/ws/chat/rooms/{roomCode}", configurator = HttpSessionConfigurator.class)
public class GlobalChatEndpoint {
    private static final Map<String, CopyOnWriteArraySet<GlobalChatEndpoint>> roomConnections = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static volatile ChatMessagePolicy messagePolicy = new ChatMessagePolicy(20, 200, Set.of());
    private static ChatMessageMapper chatMessageMapper;
    private static ChatRoomService chatRoomService;

    private Session session;
    private UserSessionDTO user;
    private String roomCode;
    private Long roomId;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam("roomCode") String roomCode) {
        this.session = session;
        this.roomCode = normalizeRoomCode(roomCode);
        if (config != null && config.getUserProperties() != null) {
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            if (httpSession != null) {
                this.user = (UserSessionDTO) httpSession.getAttribute("user");
            }
        }
        if (this.user == null) {
            closeWithPolicy("请先登录");
            return;
        }
        ensureBeans();
        if (chatRoomService == null) {
            closeWithPolicy("服务暂不可用");
            return;
        }
        ForumChatRoom room;
        try {
            room = chatRoomService.getRoomByCode(this.roomCode);
        } catch (RuntimeException ex) {
            closeWithPolicy(ex.getMessage() == null ? "群组不存在" : ex.getMessage());
            return;
        }
        if (!chatRoomService.hasJoined(room.getId(), user.getId())) {
            closeWithPolicy("加入群组后才可连接");
            return;
        }
        this.roomId = room.getId();
        roomConnections.computeIfAbsent(this.roomCode, key -> new CopyOnWriteArraySet<>()).add(this);
    }

    @OnMessage
    public void onMessage(String rawContent) {
        if (user == null || roomId == null) {
            return;
        }
        ChatMessagePolicy.ValidationResult validationResult = messagePolicy.validate(user.getId(), rawContent);
        if (!validationResult.isPassed()) {
            sendSystemMessage(validationResult.getRejectReason());
            return;
        }
        ensureBeans();
        if (chatMessageMapper == null) {
            sendSystemMessage("消息服务异常，请稍后重试");
            return;
        }
        ForumChatMessage msg = new ForumChatMessage();
        msg.setRoomId(roomId);
        msg.setUserId(user.getId());
        msg.setMessageType("TEXT");
        msg.setContent(validationResult.getContent());
        chatMessageMapper.insert(msg);

        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId() != null ? msg.getId() : System.currentTimeMillis());
        vo.setUserId(user.getId());
        vo.setContent(msg.getContent());
        vo.setCreatedAt(new Date());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoomCode(roomCode);
        try {
            String json = objectMapper.writeValueAsString(vo);
            broadcastToRoom(roomCode, json);
        } catch (Exception ignored) {
        }
    }

    @OnClose
    public void onClose() {
        removeConnection();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        removeConnection();
    }

    private void broadcastToRoom(String targetRoomCode, String messageJson) {
        CopyOnWriteArraySet<GlobalChatEndpoint> endpoints = roomConnections.get(targetRoomCode);
        if (endpoints == null || endpoints.isEmpty()) {
            return;
        }
        for (GlobalChatEndpoint endpoint : endpoints) {
            try {
                synchronized (endpoint) {
                    if (endpoint.session != null && endpoint.session.isOpen()) {
                        endpoint.session.getBasicRemote().sendText(messageJson);
                    }
                }
            } catch (Exception ex) {
                endpoints.remove(endpoint);
            }
        }
    }

    private void ensureBeans() {
        if (chatMessageMapper == null || chatRoomService == null) {
            WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
            if (ctx != null) {
                try {
                    chatMessageMapper = ctx.getBean(ChatMessageMapper.class);
                    chatRoomService = ctx.getBean(ChatRoomService.class);
                    Environment environment = ctx.getEnvironment();
                    int rateLimitPerMinute = parseInt(environment.getProperty("chat.rate.limit.per.minute"), 20);
                    int maxLength = parseInt(environment.getProperty("chat.message.max.length"), 200);
                    String sensitiveRaw = environment.getProperty("chat.sensitive.words", "");
                    messagePolicy = new ChatMessagePolicy(rateLimitPerMinute, maxLength, parseSensitiveWords(sensitiveRaw));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void closeWithPolicy(String reason) {
        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
            }
        } catch (Exception ignored) {
        }
    }

    private void removeConnection() {
        if (roomCode == null) {
            return;
        }
        CopyOnWriteArraySet<GlobalChatEndpoint> endpoints = roomConnections.get(roomCode);
        if (endpoints == null) {
            return;
        }
        endpoints.remove(this);
        if (endpoints.isEmpty()) {
            roomConnections.remove(roomCode, endpoints);
        }
    }

    private void sendSystemMessage(String content) {
        try {
            ChatMessageVO system = new ChatMessageVO();
            system.setId(System.currentTimeMillis());
            system.setUserId(0L);
            system.setNickname("系统");
            system.setAvatar("");
            system.setContent(content);
            system.setCreatedAt(new Date());
            system.setRoomCode(roomCode);
            String json = objectMapper.writeValueAsString(system);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (Exception ignored) {
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Set<String> parseSensitiveWords(String raw) {
        Set<String> result = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String[] words = raw.split(",");
        for (String word : words) {
            String normalized = word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeRoomCode(String rawRoomCode) {
        if (rawRoomCode == null || rawRoomCode.isBlank()) {
            return "";
        }
        return rawRoomCode.trim().toUpperCase(Locale.ROOT);
    }
}
