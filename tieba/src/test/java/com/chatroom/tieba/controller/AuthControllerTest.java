package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController();
        setField("userService", userService);
    }

    @Test
    void shouldExposeSafeReturnToOnLoginPage() {
        when(request.getContextPath()).thenReturn("");
        Model model = new ExtendedModelMap();

        String view = authController.showLoginForm("/chat/global?from=home", request, model);

        assertEquals("auth/login", view);
        assertEquals("/chat/global?from=home", model.getAttribute("returnTo"));
    }

    @Test
    void shouldIgnoreUnsafeReturnToOnLoginPage() {
        Model model = new ExtendedModelMap();

        String view = authController.showLoginForm("https://evil.example/steal", request, model);

        assertEquals("auth/login", view);
        assertFalse(model.containsAttribute("returnTo"));
    }

    @Test
    void shouldRedirectToSafeReturnToAfterSuccessfulLogin() {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(7L);
        when(request.getContextPath()).thenReturn("");
        when(userService.login("alice", "secret")).thenReturn(user);
        Model model = new ExtendedModelMap();

        String view = authController.processLogin("alice", "secret", "/chat/rooms/GLOBAL", session, request, model);

        assertEquals("redirect:/chat/rooms/GLOBAL", view);
        verify(session).setAttribute("user", user);
    }

    @Test
    void shouldFallbackHomeAfterSuccessfulLoginWhenReturnToIsUnsafe() {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(7L);
        when(userService.login("alice", "secret")).thenReturn(user);
        Model model = new ExtendedModelMap();

        String view = authController.processLogin("alice", "secret", "https://evil.example/steal", session, request, model);

        assertEquals("redirect:/", view);
        verify(session).setAttribute("user", user);
    }

    @Test
    void shouldPreserveSafeReturnToWhenLoginFails() {
        when(request.getContextPath()).thenReturn("");
        when(userService.login("alice", "badpass")).thenThrow(new RuntimeException("账号或密码错误"));
        Model model = new ExtendedModelMap();

        String view = authController.processLogin("alice", "badpass", "/user/profile", session, request, model);

        assertEquals("auth/login", view);
        assertEquals("账号或密码错误", model.getAttribute("error"));
        assertEquals("/user/profile", model.getAttribute("returnTo"));
    }

    @Test
    void shouldStripContextPathFromSafeReturnTo() {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(7L);
        when(request.getContextPath()).thenReturn("/tieba");
        when(userService.login("alice", "secret")).thenReturn(user);
        Model model = new ExtendedModelMap();

        String view = authController.processLogin("alice", "secret", "/tieba/chat/global", session, request, model);

        assertEquals("redirect:/chat/global", view);
    }

    @Test
    void shouldPreserveFragmentWhenRedirectingAfterSuccessfulLogin() {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(7L);
        when(request.getContextPath()).thenReturn("");
        when(userService.login("alice", "secret")).thenReturn(user);
        Model model = new ExtendedModelMap();

        String view = authController.processLogin("alice", "secret", "/chat/rooms#rooms-lobby", session, request, model);

        assertEquals("redirect:/chat/rooms#rooms-lobby", view);
    }

    @Test
    void loginTemplateShouldKeepHiddenReturnToEscapedFlashMessagesAndContinueHint() throws Exception {
        String template = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp/WEB-INF/jsp/auth/login.jsp"),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(template.contains("<c:out value=\"${error}\" />"));
        assertTrue(template.contains("<c:out value=\"${msg}\" />"));
        assertTrue(template.contains("name=\"returnTo\""));
        assertTrue(template.contains("登录后将继续访问上一页。"));
        assertFalse(template.contains("<div class=\"alert alert-error\">${error}</div>"));
    }

    @Test
    void profileTemplateShouldEscapeFlashMessagesAndExposeUnifiedPostingEntry() throws Exception {
        String template = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp/WEB-INF/jsp/user/profile.jsp"),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(template.contains("<c:out value=\"${error}\" />"));
        assertTrue(template.contains("<c:out value=\"${success}\" />"));
        assertTrue(template.contains(">我要发帖<"));
        assertTrue(template.contains("<c:param name=\"entrySource\" value=\"profile\" />"));
        assertTrue(template.contains("统一发帖页"));
        assertFalse(template.contains("${error}</div>"));
        assertFalse(template.contains("${success}</div>"));
    }

    @Test
    void headerTemplateShouldExposeAnonymousProtectedLinksWithReturnTo() throws Exception {
        String template = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp/WEB-INF/jsp/common/header.jsp"),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(template.contains("<c:param name=\"returnTo\" value=\"/chat/rooms#rooms-lobby\" />"));
        assertTrue(template.contains("<c:set var=\"profileUrl\" value=\"${pageContext.request.contextPath}/user/profile\" />"));
        assertTrue(template.contains("<a href=\"${interestGroupsUrl}\">兴趣群组</a>"));
    }

    @Test
    void indexTemplateShouldRouteSecondaryCtaToInterestGroups() throws Exception {
        String template = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp/WEB-INF/jsp/index.jsp"),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(template.contains("我要发帖"));
        assertTrue(template.contains("${postEntryUrl}"));
        assertTrue(template.contains("<c:set var=\"postEntryUrl\" value=\"${pageContext.request.contextPath}${postEntryPath}\" />"));
        assertTrue(template.contains("浏览兴趣群组"));
        assertTrue(template.contains("${interestGroupsEntryUrl}"));
        assertFalse(template.contains("name=\"returnTo\" value=\"${postEntryPath}\""));
        assertFalse(template.contains("href=\"#board-zone\" class=\"btn btn-ghost\">浏览兴趣分区</a>"));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = AuthController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(authController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
