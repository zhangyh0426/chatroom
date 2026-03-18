package com.chatroom.tieba.controller;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import com.chatroom.tieba.service.ForumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private ForumService forumService;

    @GetMapping("/")
    public String index(Model model) {
        Map<ForumCategory, List<ForumBoard>> indexData = forumService.getIndexData();
        model.addAttribute("indexData", indexData);
        return "index";
    }
}