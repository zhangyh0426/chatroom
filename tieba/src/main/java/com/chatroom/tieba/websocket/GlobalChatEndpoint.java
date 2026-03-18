package com.chatroom.tieba.websocket;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumChatMessage;
import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/ws/chat/global", configurator = HttpSessionConfigurator.class)
public class GlobalChatEndpoint {

    private static final Set<GlobalChatEndpoint> connections = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private Session session;
    private UserSessionDTO user;
    
    // 注：由于 WebSocket 端点由 Tomcat 直接管理，无法通过 Spring @Autowired 注入
    // 我们需要在第一调用时手动从 Spring 上下文中获取 Bean
    private static ChatMessageMapper chatMessageMapper;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        
        if (config != null && config.getUserProperties() != null) {
            HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
            if (httpSession != null) {
                this.user = (UserSessionDTO) httpSession.getAttribute("user");
            }
        }
        
        if (this.user == null) {
            try { 
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Login required")); 
            } catch (Exception e) {
                // Ignore
            }
            return;
        }
        
        connections.add(this);
        ensureMapper();
        
        // 可选广播: 欢迎某人进入聊天室
    }

    @OnMessage
    public void onMessage(String rawContent) {
        if (user == null || rawContent == null || rawContent.trim().isEmpty()) {
            return;
        }
        
        // 1. 保存到 MySQL 数据库
        ForumChatMessage msg = new ForumChatMessage();
        msg.setRoomId(1); // 约定1为 GLOBAL 大厅
        msg.setUserId(user.getId());
        msg.setMessageType("TEXT");
        msg.setContent(rawContent.trim());
        
        ensureMapper();
        if (chatMessageMapper != null) {
            chatMessageMapper.insert(msg);
        }
        
        // 2. 构造要发往所有客户端的 JSON
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId() != null ? msg.getId() : System.currentTimeMillis());
        vo.setUserId(user.getId());
        vo.setContent(msg.getContent());
        vo.setCreatedAt(new Date()); 
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        
        try {
            String json = objectMapper.writeValueAsString(vo);
            broadcast(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose() {
        connections.remove(this);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        connections.remove(this);
    }

    private void broadcast(String messageJson) {
        for (GlobalChatEndpoint endpoint : connections) {
            try {
                // 同步发送，防止并发导致异常
                synchronized (endpoint) {
                    if (endpoint.session.isOpen()) {
                        endpoint.session.getBasicRemote().sendText(messageJson);
                    }
                }
            } catch (Exception e) {
                connections.remove(endpoint);
            }
        }
    }

    private void ensureMapper() {
        if (chatMessageMapper == null) {
            WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
            if (ctx != null) {
                try {
                    chatMessageMapper = ctx.getBean(ChatMessageMapper.class);
                } catch (Exception e) {
                    // fall back
                }
            }
        }
    }
}