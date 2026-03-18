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

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpSession session,
                               Model model) {
        try {
            UserSessionDTO user = userService.login(username, password);
            session.setAttribute("user", user);
            return "redirect:/"; // 登录成功跳转首页
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
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
}