package com.chatroom.tieba.controller;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.ThreadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexControllerTest {

    @Mock
    private ForumService forumService;

    private IndexController indexController;

    @BeforeEach
    void setUp() {
        indexController = new IndexController();
        setField("forumService", forumService);
    }

    @Test
    void shouldExposePostEntryPathWithFirstBoardId() {
        ForumBoard board = new ForumBoard();
        board.setId(5);
        Map<ForumCategory, List<ForumBoard>> indexData = new LinkedHashMap<>();
        indexData.put(new ForumCategory(), List.of(board));
        ThreadVO latest = new ThreadVO();
        latest.setId(11L);
        when(forumService.getIndexData()).thenReturn(indexData);
        when(forumService.getLatestThreads(6)).thenReturn(List.of(latest));
        when(forumService.getHotThreads(6)).thenReturn(List.of());
        when(forumService.getEssenceThreads(6)).thenReturn(List.of());
        when(forumService.getActivityThreads(6)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = indexController.index(model);

        assertEquals("index", view);
        assertEquals(indexData, model.getAttribute("indexData"));
        assertEquals("/board/post/thread?boardId=5&entrySource=home", model.getAttribute("postEntryPath"));
        assertEquals(List.of(latest), model.getAttribute("latestThreads"));
    }

    @Test
    void shouldFallbackPostEntryPathWhenNoBoard() {
        when(forumService.getIndexData()).thenReturn(Map.of());
        when(forumService.getLatestThreads(6)).thenReturn(List.of());
        when(forumService.getHotThreads(6)).thenReturn(List.of());
        when(forumService.getEssenceThreads(6)).thenReturn(List.of());
        when(forumService.getActivityThreads(6)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = indexController.index(model);

        assertEquals("index", view);
        assertEquals("/board/post/thread?entrySource=home", model.getAttribute("postEntryPath"));
        assertEquals(List.of(), model.getAttribute("activityThreads"));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = IndexController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(indexController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
