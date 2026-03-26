package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalViewAdvice {

    @Autowired
    private NotificationService notificationService;

    @ModelAttribute("headerUnreadNotificationCount")
    public int headerUnreadNotificationCount(HttpSession session) {
        UserSessionDTO user = session == null ? null : (UserSessionDTO) session.getAttribute("user");
        if (user == null || user.getId() == null) {
            return 0;
        }
        return notificationService.getUnreadCount(user.getId());
    }
}
