package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/thread")
public class ThreadController {

    @Autowired
    private ForumService forumService;

    @GetMapping("/{threadId}")
    public String viewThread(@PathVariable("threadId") Long threadId, Model model) {
        ThreadVO thread = forumService.getThreadWithViewCountInc(threadId);
        List<PostVO> posts = forumService.getPostsWithReplies(threadId);
        model.addAttribute("thread", thread);
        model.addAttribute("posts", posts);
        model.addAttribute("boardId", thread.getBoardId());
        return "thread/thread";
    }

    @PostMapping("/reply")
    public String createReply(@RequestParam("threadId") Long threadId,
                              @RequestParam("content") String content,
                              HttpSession session) {
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        forumService.createPost(threadId, user.getId(), content);
        return "redirect:/thread/" + threadId; // 简单重定向，默认跳回原帖首页面
    }
    
    @PostMapping("/subreply")
    public String createSubReply(@RequestParam("threadId") Long threadId,
                                 @RequestParam("postId") Long postId,
                                 @RequestParam(value="replyToUserId", required=false) Long replyToUserId,
                                 @RequestParam("content") String content,
                                 HttpSession session) {
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        forumService.createReply(threadId, postId, user.getId(), replyToUserId, content);
        return "redirect:/thread/" + threadId;
    }
}