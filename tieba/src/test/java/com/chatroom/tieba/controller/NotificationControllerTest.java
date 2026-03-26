package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.NotificationService;
import com.chatroom.tieba.vo.NotificationVO;
import com.chatroom.tieba.vo.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private HttpSession session;

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController();
        setField("notificationService", notificationService);
    }

    @Test
    void shouldShowNotificationListForCurrentUser() {
        UserSessionDTO user = new UserSessionDTO(7L, "alice", "Alice", null);
        NotificationVO notification = new NotificationVO();
        notification.setId(101L);
        PageResult<NotificationVO> pageResult = new PageResult<>(List.of(notification), 1, 10, 1);
        when(session.getAttribute("user")).thenReturn(user);
        when(notificationService.getNotifications(7L, 1, 10)).thenReturn(pageResult);
        Model model = new ExtendedModelMap();

        String view = notificationController.notifications(session, model, 1, 10);

        assertEquals("user/notifications", view);
        assertEquals(pageResult, model.getAttribute("pageResult"));
        assertEquals(List.of(notification), model.getAttribute("notifications"));
        verify(notificationService).getNotifications(7L, 1, 10);
    }

    @Test
    void shouldMarkAllNotificationsRead() {
        UserSessionDTO user = new UserSessionDTO(7L, "alice", "Alice", null);
        when(session.getAttribute("user")).thenReturn(user);

        String view = notificationController.markAllRead(session);

        assertEquals("redirect:/user/notifications", view);
        verify(notificationService).markAllRead(7L);
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = NotificationController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(notificationController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
