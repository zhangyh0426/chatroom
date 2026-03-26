package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumLikeLog;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadImageVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/thread")
public class ThreadController {

    @Autowired
    private ForumService forumService;

    @GetMapping("/{threadId}")
    public String viewThread(@PathVariable("threadId") Long threadId,
                             @RequestParam(value = "fromProfile", defaultValue = "false") boolean fromProfile,
                             Model model) {
        ThreadVO thread = forumService.getThreadWithViewCountInc(threadId);
        List<PostVO> posts = forumService.getPostsWithReplies(threadId);
        List<ThreadImageVO> threadImages = forumService.getThreadImages(threadId);
        thread.setImages(threadImages);
        model.addAttribute("thread", thread);
        model.addAttribute("posts", posts);
        model.addAttribute("threadImages", threadImages);
        model.addAttribute("boardId", thread.getBoardId());
        model.addAttribute("fromProfile", fromProfile);
        return "thread/thread";
    }

    @PostMapping("/reply")
    public String createReply(@RequestParam("threadId") Long threadId,
                              @RequestParam("content") String content,
                              HttpSession session) {
        UserSessionDTO user = requireLogin(session);
        Long postId = forumService.createPost(threadId, user.getId(), content);
        return "redirect:/thread/" + threadId + "#post-" + postId;
    }
    
    @PostMapping("/subreply")
    public String createSubReply(@RequestParam("threadId") Long threadId,
                                 @RequestParam("postId") Long postId,
                                 @RequestParam(value="replyToUserId", required=false) Long replyToUserId,
                                 @RequestParam("content") String content,
                                 HttpSession session) {
        UserSessionDTO user = requireLogin(session);
        forumService.createReply(threadId, postId, user.getId(), replyToUserId, content);
        return "redirect:/thread/" + threadId + "#post-" + postId;
    }

    @PostMapping("/api/{threadId}/like")
    @ResponseBody
    public Map<String, Object> likeThread(@PathVariable("threadId") Long threadId, HttpSession session) {
        UserSessionDTO user = requireLogin(session);
        if (threadId == null || threadId <= 0) {
            throw new RuntimeException("帖子参数不合法");
        }
        ForumLikeLog likeLog = forumService.likeThread(threadId, user.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("liked", likeLog != null);
        payload.put("message", likeLog == null ? "你已经点过赞了" : "点赞成功");
        return payload;
    }

    @PostMapping("/api/{threadId}/delete")
    @ResponseBody
    public Map<String, Object> deleteThread(@PathVariable("threadId") Long threadId, HttpSession session) {
        UserSessionDTO user = requireLogin(session);
        if (threadId == null || threadId <= 0) {
            throw new RuntimeException("帖子参数不合法");
        }
        boolean deleted = forumService.deleteThreadByAuthor(threadId, user.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("deleted", deleted);
        payload.put("message", deleted ? "删除成功" : "帖子已删除");
        return payload;
    }

    private UserSessionDTO requireLogin(HttpSession session) {
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }
}
