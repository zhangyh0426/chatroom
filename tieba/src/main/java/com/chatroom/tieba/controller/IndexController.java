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
        model.addAttribute("postEntryPath", resolvePostEntryPath(indexData));
        model.addAttribute("latestThreads", forumService.getLatestThreads(6));
        model.addAttribute("hotThreads", forumService.getHotThreads(6));
        model.addAttribute("essenceThreads", forumService.getEssenceThreads(6));
        model.addAttribute("activityThreads", forumService.getActivityThreads(6));
        return "index";
    }

    private String resolvePostEntryPath(Map<ForumCategory, List<ForumBoard>> indexData) {
        if (indexData == null || indexData.isEmpty()) {
            return "/board/post/thread?entrySource=home";
        }
        for (List<ForumBoard> boards : indexData.values()) {
            if (boards == null || boards.isEmpty()) {
                continue;
            }
            ForumBoard board = boards.get(0);
            if (board != null && board.getId() != null) {
                return "/board/post/thread?boardId=" + board.getId() + "&entrySource=home";
            }
        }
        return "/board/post/thread?entrySource=home";
    }
}
