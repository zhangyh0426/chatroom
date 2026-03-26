package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.NotificationService;
import com.chatroom.tieba.vo.NotificationVO;
import com.chatroom.tieba.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String notifications(HttpSession session,
                                Model model,
                                @org.springframework.web.bind.annotation.RequestParam(value = "page", defaultValue = "1") int page,
                                @org.springframework.web.bind.annotation.RequestParam(value = "size", defaultValue = "10") int size) {
        UserSessionDTO user = currentUser(session);
        PageResult<NotificationVO> pageResult = notificationService.getNotifications(user.getId(), page, size);
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("notifications", pageResult.getList());
        return "user/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(HttpSession session) {
        UserSessionDTO user = currentUser(session);
        notificationService.markAllRead(user.getId());
        return "redirect:/user/notifications";
    }

    private UserSessionDTO currentUser(HttpSession session) {
        UserSessionDTO user = session == null ? null : (UserSessionDTO) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }
}
