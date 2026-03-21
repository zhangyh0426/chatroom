package com.chatroom.tieba.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public Object handleRuntime(RuntimeException ex,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) throws IOException {
        if (isApiRequest(request)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", false);
            payload.put("code", "BAD_REQUEST");
            payload.put("message", ex.getMessage());
            return payload;
        }
        if (ex.getMessage() != null && ex.getMessage().contains("请先登录")) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return null;
        }
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        response.sendRedirect(request.getHeader("Referer") == null ? request.getContextPath() + "/" : request.getHeader("Referer"));
        return null;
    }

    private boolean isApiRequest(HttpServletRequest request) {
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
}
