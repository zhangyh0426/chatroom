package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.service.UserService;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadVO;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ForumService forumService;

    @Mock
    private HttpSession session;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        setField("userService", userService);
        setField("forumService", forumService);
    }

    @Test
    void shouldShowProfileInGuestMode() {
        when(session.getAttribute("user")).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = userController.showProfile(1, 1, null, session, model);

        assertEquals("user/profile", view);
        assertFalse(model.containsAttribute("profile"));
    }

    @Test
    void shouldLoadProfileAndFootprintsWhenLoggedIn() {
        UserSessionDTO user = new UserSessionDTO(7L, "alice", "Alice", "/uploads/a.png");
        when(session.getAttribute("user")).thenReturn(user);
        UserProfile profile = new UserProfile();
        profile.setUserId(7L);
        profile.setNickname("Alice");
        when(userService.getProfileByUserId(7L)).thenReturn(profile);
        PageResult<ThreadVO> threadPage = new PageResult<>(List.of(), 1, 5, 0);
        PageResult<PostVO> replyPage = new PageResult<>(List.of(), 1, 5, 0);
        when(forumService.getThreadsByUser(7L, 1, 5)).thenReturn(threadPage);
        when(forumService.getPostsByUser(7L, 1, 5)).thenReturn(replyPage);
        Model model = new ExtendedModelMap();

        String view = userController.showProfile(1, 1, 99L, session, model);

        assertEquals("user/profile", view);
        assertEquals(profile, model.getAttribute("profile"));
        assertEquals(user, model.getAttribute("account"));
        assertEquals(threadPage, model.getAttribute("myThreads"));
        assertEquals(replyPage, model.getAttribute("myReplies"));
        assertEquals(99L, model.getAttribute("highlightThreadId"));
        verify(forumService).getThreadsByUser(7L, 1, 5);
        verify(forumService).getPostsByUser(7L, 1, 5);
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = UserController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(userController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
