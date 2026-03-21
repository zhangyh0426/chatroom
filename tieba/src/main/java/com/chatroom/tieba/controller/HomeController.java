package com.chatroom.tieba.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Locale;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "redirect:/";
    }

    @GetMapping("/chat")
    public String chat() {
        return "redirect:/chat/global";
    }

    @GetMapping("/chatrooms")
    public String legacyChatRooms() {
        return "redirect:/chat/rooms";
    }

    @GetMapping("/chatrooms/{roomCode}")
    public String legacyChatRoomDetail(@PathVariable("roomCode") String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            return "redirect:/chat/rooms";
        }
        return "redirect:/chat/rooms/" + roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
