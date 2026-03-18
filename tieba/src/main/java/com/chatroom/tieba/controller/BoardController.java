package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private ForumService forumService;

    @GetMapping("/{boardId}")
    public String goBoard(@PathVariable("boardId") Integer boardId, Model model) {
        ForumBoard board = forumService.getBoardById(boardId);
        List<ThreadVO> threads = forumService.getThreadsByBoard(boardId);
        model.addAttribute("board", board);
        model.addAttribute("threads", threads);
        return "board/board";
    }

    @PostMapping("/post/thread")
    public String createThread(@RequestParam("boardId") Integer boardId,
                               @RequestParam("title") String title,
                               @RequestParam("content") String content,
                               HttpSession session) {
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        Long threadId = forumService.createThread(boardId, user.getId(), title, content);
        return "redirect:/thread/" + threadId;
    }
}