package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URI;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "returnTo", required = false) String returnTo,
                                HttpServletRequest request,
                                Model model) {
        exposeReturnTo(model, sanitizeReturnTo(returnTo, request));
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               HttpSession session,
                               HttpServletRequest request,
                               Model model) {
        String safeReturnTo = sanitizeReturnTo(returnTo, request);
        try {
            UserSessionDTO user = userService.login(username, password);
            session.setAttribute("user", user);
            return safeReturnTo == null ? "redirect:/" : "redirect:" + safeReturnTo;
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            exposeReturnTo(model, safeReturnTo);
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegisterForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam("nickname") String nickname,
                                  Model model) {
        try {
            userService.register(username, password, nickname);
            model.addAttribute("msg", "注册成功，请登录");
            return "auth/login";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private void exposeReturnTo(Model model, String returnTo) {
        if (returnTo != null) {
            model.addAttribute("returnTo", returnTo);
        }
    }

    private String sanitizeReturnTo(String returnTo, HttpServletRequest request) {
        if (returnTo == null) {
            return null;
        }
        String candidate = returnTo.trim();
        if (candidate.isEmpty() || candidate.contains("\r") || candidate.contains("\n") || candidate.startsWith("//")) {
            return null;
        }
        try {
            URI uri = URI.create(candidate);
            if (uri.isAbsolute()) {
                return null;
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith("/")) {
                return null;
            }
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (path.equals(contextPath)) {
                    path = "/";
                } else if (path.startsWith(contextPath + "/")) {
                    path = path.substring(contextPath.length());
                }
            }
            if (path.startsWith("//") || "/auth/login".equals(path) || "/auth/logout".equals(path)) {
                return null;
            }
            StringBuilder normalized = new StringBuilder(path);
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                normalized.append('?').append(uri.getRawQuery());
            }
            if (uri.getRawFragment() != null && !uri.getRawFragment().isBlank()) {
                normalized.append('#').append(uri.getRawFragment());
            }
            return normalized.toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
