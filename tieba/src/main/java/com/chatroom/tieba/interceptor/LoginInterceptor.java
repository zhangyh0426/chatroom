package com.chatroom.tieba.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import com.chatroom.tieba.dto.UserSessionDTO;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
            if (user != null) {
                return true;
            }
        }
        
        if (shouldReturnJson(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"请先登录\"}");
            return false;
        }

        response.sendRedirect(buildLoginRedirect(request));
        return false;
    }

    private boolean shouldReturnJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (isApiStyleUri(request, uri)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private boolean isApiStyleUri(HttpServletRequest request, String uri) {
        if (uri == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return path.startsWith("/api/") || path.contains("/api/");
    }

    private String buildLoginRedirect(HttpServletRequest request) {
        StringBuilder location = new StringBuilder();
        String contextPath = request.getContextPath();
        if (contextPath != null) {
            location.append(contextPath);
        }
        location.append("/auth/login");
        String returnTo = resolveReturnTo(request);
        if (returnTo != null && !returnTo.isBlank()) {
            location.append("?returnTo=")
                    .append(URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
        }
        return location.toString();
    }

    private String resolveReturnTo(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            return buildCurrentPath(request);
        }
        return resolveRefererPath(request);
    }

    private String buildCurrentPath(HttpServletRequest request) {
        String path = stripContextPath(request.getRequestURI(), request.getContextPath());
        if (path == null || path.isBlank()) {
            path = "/";
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            path = path + "?" + query;
        }
        return path;
    }

    private String resolveRefererPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI refererUri = URI.create(referer);
            if (!isSameOrigin(request, refererUri)) {
                return "/";
            }
            String path = stripContextPath(refererUri.getRawPath(), request.getContextPath());
            if (path == null || path.isBlank()) {
                path = "/";
            }
            String query = refererUri.getRawQuery();
            if (query != null && !query.isBlank()) {
                path = path + "?" + query;
            }
            return path;
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }

    private boolean isSameOrigin(HttpServletRequest request, URI uri) {
        if (uri == null) {
            return false;
        }
        if (!uri.isAbsolute()) {
            String path = uri.getRawPath();
            return path != null && path.startsWith("/");
        }
        return equalsIgnoreCase(request.getScheme(), uri.getScheme())
                && equalsIgnoreCase(request.getServerName(), uri.getHost())
                && normalizePort(request.getScheme(), request.getServerPort()) == normalizePort(uri.getScheme(), uri.getPort());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }

    private int normalizePort(String scheme, int port) {
        if (port > 0) {
            return port;
        }
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private String stripContextPath(String value, String contextPath) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        if (contextPath != null && !contextPath.isEmpty()) {
            if (value.equals(contextPath)) {
                return "/";
            }
            if (value.startsWith(contextPath + "/")) {
                return value.substring(contextPath.length());
            }
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
