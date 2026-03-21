package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PageResult;
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
    public String goBoard(@PathVariable("boardId") Integer boardId,
                          @RequestParam(value = "page", defaultValue = "1") int page,
                          @RequestParam(value = "size", defaultValue = "10") int size,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          Model model) {
        ForumBoard board = forumService.getBoardById(boardId);
        PageResult<ThreadVO> pageResult = forumService.getThreadsByBoardPaged(boardId, keyword, page, size);
        List<ThreadVO> hotThreads = forumService.getHotThreads(10);
        model.addAttribute("board", board);
        model.addAttribute("threads", pageResult.getList());
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("hotThreads", hotThreads);
        return "board/board";
    }

    @PostMapping("/post/thread")
    public String createThread(@RequestParam("boardId") Integer boardId,
                               @RequestParam("title") String title,
                               @RequestParam("content") String content,
                               HttpSession session) {
        UserSessionDTO user = requireLogin(session);
        Long threadId = forumService.createThread(boardId, user.getId(), title, content);
        return "redirect:/thread/" + threadId;
    }

    private UserSessionDTO requireLogin(HttpSession session) {
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }
}
