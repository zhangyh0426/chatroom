package com.chatroom.tieba.websocket;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumChatMessage;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.service.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GlobalChatEndpointTest {

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private EndpointConfig endpointConfig;

    @Mock
    private HttpSession httpSession;

    @Mock
    private Session wsSession;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    @BeforeEach
    void setUp() {
        setStaticField("chatRoomService", chatRoomService);
        setStaticField("chatMessageMapper", chatMessageMapper);
        setStaticField("messagePolicy", new ChatMessagePolicy(20, 200, Set.of()));
        clearRoomConnections();
    }

    @Test
    void shouldAllowJoinedUserConnectToRoom() throws Exception {
        UserSessionDTO user = buildUser(1L, "Alice");
        mockHttpSessionUser(user);
        when(endpointConfig.getUserProperties()).thenReturn(buildUserProperties());
        ForumChatRoom room = new ForumChatRoom();
        room.setId(11L);
        room.setRoomCode("TECH");
        room.setStatus("ENABLED");
        when(chatRoomService.getRoomByCode("TECH")).thenReturn(room);
        when(chatRoomService.hasJoined(11L, 1L)).thenReturn(true);

        GlobalChatEndpoint endpoint = new GlobalChatEndpoint();
        endpoint.onOpen(wsSession, endpointConfig, "tech");

        assertTrue(isConnectionPresent("TECH", endpoint));
        assertEquals(11L, getRoomId(endpoint));
    }

    @Test
    void shouldRejectConnectionWhenUserNotJoined() throws Exception {
        when(wsSession.isOpen()).thenReturn(true);
        UserSessionDTO user = buildUser(2L, "Bob");
        mockHttpSessionUser(user);
        when(endpointConfig.getUserProperties()).thenReturn(buildUserProperties());
        ForumChatRoom room = new ForumChatRoom();
        room.setId(7L);
        room.setRoomCode("TECH");
        room.setStatus("ENABLED");
        when(chatRoomService.getRoomByCode("TECH")).thenReturn(room);
        when(chatRoomService.hasJoined(7L, 2L)).thenReturn(false);

        GlobalChatEndpoint endpoint = new GlobalChatEndpoint();
        endpoint.onOpen(wsSession, endpointConfig, "TECH");

        ArgumentCaptor<CloseReason> closeReasonCaptor = ArgumentCaptor.forClass(CloseReason.class);
        verify(wsSession).close(closeReasonCaptor.capture());
        assertEquals("加入群组后才可连接", closeReasonCaptor.getValue().getReasonPhrase());
        assertFalse(isConnectionPresent("TECH", endpoint));
    }

    @Test
    void shouldBroadcastOnlyInsideCurrentRoomAndPersistCorrectRoomId() throws Exception {
        Session senderSession = mockOpenSession();
        Session techReceiverSession = mockOpenSession();
        Session gameReceiverSession = mockOpenSession();
        UserSessionDTO senderUser = buildUser(3L, "Sender");
        mockHttpSessionUser(senderUser);
        when(endpointConfig.getUserProperties()).thenReturn(buildUserProperties());

        GlobalChatEndpoint sender = openJoinedEndpoint(senderSession, "TECH", 100L, 3L);
        GlobalChatEndpoint techReceiver = openJoinedEndpoint(techReceiverSession, "TECH", 100L, 4L);
        GlobalChatEndpoint gameReceiver = openJoinedEndpoint(gameReceiverSession, "GAME", 200L, 5L);

        sender.onMessage("hello tech");

        ArgumentCaptor<ForumChatMessage> messageCaptor = ArgumentCaptor.forClass(ForumChatMessage.class);
        verify(chatMessageMapper).insert(messageCaptor.capture());
        ForumChatMessage saved = messageCaptor.getValue();
        assertEquals(100L, saved.getRoomId());
        assertEquals(3L, saved.getUserId());
        assertEquals("hello tech", saved.getContent());
        verify(getBasicRemote(senderSession)).sendText(any(String.class));
        verify(getBasicRemote(techReceiverSession)).sendText(any(String.class));
        verify(getBasicRemote(gameReceiverSession), never()).sendText(any(String.class));
        assertTrue(isConnectionPresent("TECH", sender));
        assertTrue(isConnectionPresent("TECH", techReceiver));
        assertTrue(isConnectionPresent("GAME", gameReceiver));
    }

    @Test
    void shouldApplyValidationPolicyAndRejectTooLongMessage() throws Exception {
        setStaticField("messagePolicy", new ChatMessagePolicy(20, 5, Set.of("ban")));
        Session senderSession = mockOpenSession();
        GlobalChatEndpoint sender = openJoinedEndpoint(senderSession, "TECH", 300L, 9L);

        sender.onMessage("123456");

        verify(chatMessageMapper, never()).insert(any(ForumChatMessage.class));
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(getBasicRemote(senderSession)).sendText(payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().contains("消息过长"));
    }

    private GlobalChatEndpoint openJoinedEndpoint(Session session, String roomCode, Long roomId, Long userId) {
        UserSessionDTO user = buildUser(userId, "User-" + userId);
        mockHttpSessionUser(user);
        when(endpointConfig.getUserProperties()).thenReturn(buildUserProperties());
        ForumChatRoom room = new ForumChatRoom();
        room.setId(roomId);
        room.setRoomCode(roomCode);
        room.setStatus("ENABLED");
        when(chatRoomService.getRoomByCode(roomCode)).thenReturn(room);
        when(chatRoomService.hasJoined(roomId, userId)).thenReturn(true);
        GlobalChatEndpoint endpoint = new GlobalChatEndpoint();
        endpoint.onOpen(session, endpointConfig, roomCode);
        return endpoint;
    }

    private Session mockOpenSession() throws Exception {
        Session session = org.mockito.Mockito.mock(Session.class);
        RemoteEndpoint.Basic remote = org.mockito.Mockito.mock(RemoteEndpoint.Basic.class);
        lenient().when(session.isOpen()).thenReturn(true);
        when(session.getBasicRemote()).thenReturn(remote);
        return session;
    }

    private RemoteEndpoint.Basic getBasicRemote(Session session) {
        try {
            return session.getBasicRemote();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private UserSessionDTO buildUser(Long userId, String nickname) {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(userId);
        user.setNickname(nickname);
        user.setAvatar("/uploads/" + userId + ".png");
        return user;
    }

    private void mockHttpSessionUser(UserSessionDTO user) {
        when(httpSession.getAttribute("user")).thenReturn(user);
    }

    private Map<String, Object> buildUserProperties() {
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put(HttpSession.class.getName(), httpSession);
        return userProperties;
    }

    @SuppressWarnings("unchecked")
    private void clearRoomConnections() {
        try {
            Field field = GlobalChatEndpoint.class.getDeclaredField("roomConnections");
            field.setAccessible(true);
            Map<String, CopyOnWriteArraySet<GlobalChatEndpoint>> map =
                    (Map<String, CopyOnWriteArraySet<GlobalChatEndpoint>>) field.get(null);
            map.clear();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isConnectionPresent(String roomCode, GlobalChatEndpoint endpoint) {
        try {
            Field field = GlobalChatEndpoint.class.getDeclaredField("roomConnections");
            field.setAccessible(true);
            Map<String, CopyOnWriteArraySet<GlobalChatEndpoint>> map =
                    (Map<String, CopyOnWriteArraySet<GlobalChatEndpoint>>) field.get(null);
            CopyOnWriteArraySet<GlobalChatEndpoint> endpoints = map.get(roomCode);
            return endpoints != null && endpoints.contains(endpoint);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long getRoomId(GlobalChatEndpoint endpoint) {
        try {
            Field field = GlobalChatEndpoint.class.getDeclaredField("roomId");
            field.setAccessible(true);
            Object value = field.get(endpoint);
            assertNotNull(value);
            return (Long) value;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void setStaticField(String fieldName, Object value) {
        try {
            Field field = GlobalChatEndpoint.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
