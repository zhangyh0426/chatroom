package com.chatroom.tieba.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
        
        // 判断是否是Ajax请求，如果是则可以返回JSON格式错误提示
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(requestedWith)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized - Please login first.\"}");
            return false;
        }
        
        // 否则跳转到登录请求
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return false;
    }
}