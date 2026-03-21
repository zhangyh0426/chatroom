package com.chatroom.tieba.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void shouldRedirectChatEntryToGlobalGateway() {
        assertEquals("redirect:/chat/global", homeController.chat());
    }

    @Test
    void shouldRedirectLegacyChatRoomsEntryToRoomList() {
        assertEquals("redirect:/chat/rooms", homeController.legacyChatRooms());
    }

    @Test
    void shouldRedirectLegacyRoomDetailToNormalizedPath() {
        assertEquals("redirect:/chat/rooms/TECH", homeController.legacyChatRoomDetail("tech"));
    }
}
