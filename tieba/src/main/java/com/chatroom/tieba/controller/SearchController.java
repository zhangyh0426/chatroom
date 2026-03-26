package com.chatroom.tieba.controller;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.support.ThreadTypeCatalog;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    private ForumService forumService;

    @GetMapping("/search")
    public String search(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "boardId", required = false) Integer boardId,
                         @RequestParam(value = "threadType", required = false) String threadType,
                         @RequestParam(value = "tag", required = false) String tag,
                         @RequestParam(value = "page", defaultValue = "1") int page,
                         @RequestParam(value = "size", defaultValue = "12") int size,
                         Model model) {
        PageResult<ThreadVO> pageResult = forumService.searchThreads(keyword, boardId, threadType, tag, page, size);
        List<ForumBoard> boardOptions = forumService.getAllBoards();
        model.addAttribute("pageResult", pageResult);
        model.addAttribute("threads", pageResult.getList());
        model.addAttribute("boardOptions", boardOptions);
        model.addAttribute("threadTypeOptions", ThreadTypeCatalog.options());
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("selectedBoardId", boardId);
        model.addAttribute("threadType", threadType == null ? "" : threadType.trim());
        model.addAttribute("tag", tag == null ? "" : tag.trim());
        return "search";
    }
}
