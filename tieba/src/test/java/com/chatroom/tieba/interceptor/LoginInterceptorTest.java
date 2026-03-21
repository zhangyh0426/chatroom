package com.chatroom.tieba.interceptor;

import com.chatroom.tieba.dto.UserSessionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginInterceptorTest {

    private LoginInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        interceptor = new LoginInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
    }

    @Test
    void shouldAllowWhenLoggedInUserExists() throws Exception {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(1L);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRedirectToLoginWhenAnonymousHtmlRequest() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        when(request.getHeader("Accept")).thenReturn("text/html");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/chat/rooms");
        when(request.getQueryString()).thenReturn("tab=lobby");
        when(request.getContextPath()).thenReturn("");

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        verify(response).sendRedirect("/auth/login?returnTo=%2Fchat%2Frooms%3Ftab%3Dlobby");
    }

    @Test
    void shouldRedirectAnonymousPostRequestToLoginWithSameOriginReferer() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        when(request.getHeader("Accept")).thenReturn("text/html");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/chat/rooms/create");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader("Referer")).thenReturn("http://localhost/chat/rooms/manage?draft=1");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        verify(response).sendRedirect("/auth/login?returnTo=%2Fchat%2Frooms%2Fmanage%3Fdraft%3D1");
    }

    @Test
    void shouldRedirectAnonymousPostRequestToLoginAndFallbackHomeWhenRefererIsExternal() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        when(request.getHeader("Accept")).thenReturn("text/html");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/user/profile/update");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader("Referer")).thenReturn("https://evil.example/profile");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        verify(response).sendRedirect("/auth/login?returnTo=%2F");
    }

    @Test
    void shouldReturnJsonUnauthorizedForApiRequest() throws Exception {
        StringWriter writer = new StringWriter();
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        when(request.getHeader("Accept")).thenReturn("application/json");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/thread/like");
        when(request.getContextPath()).thenReturn("");
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(writer.toString().contains("\"code\":\"UNAUTHORIZED\""));
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }
}
