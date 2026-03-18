package com.chatroom.tieba.controller;

import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.vo.ChatMessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @GetMapping("/global")
    public String globalChat(Model model) {
        // 取出数据库记录的最近 50 条消息
        List<ChatMessageVO> history = chatMessageMapper.findRecentGlobalMessages(50);
        // Mybatis 查出来是倒序（新->旧），在UI中显示聊天应该旧在上新在下，所以执行一次翻转反转
        Collections.reverse(history);
        model.addAttribute("history", history);
        return "chat/global";
    }
}