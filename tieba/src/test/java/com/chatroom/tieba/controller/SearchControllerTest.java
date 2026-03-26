package com.chatroom.tieba.controller;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.ThreadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private ForumService forumService;

    private SearchController searchController;

    @BeforeEach
    void setUp() {
        searchController = new SearchController();
        setField("forumService", forumService);
    }

    @Test
    void shouldExposeSearchFiltersAndResults() {
        ThreadVO thread = new ThreadVO();
        thread.setId(9L);
        PageResult<ThreadVO> pageResult = new PageResult<>(List.of(thread), 2, 12, 13);
        ForumBoard board = new ForumBoard();
        board.setId(3);
        board.setName("校园杂谈");
        when(forumService.searchThreads(" 活动 ", 3, "ACTIVITY", " 社团 ", 2, 12)).thenReturn(pageResult);
        when(forumService.getAllBoards()).thenReturn(List.of(board));
        Model model = new ExtendedModelMap();

        String view = searchController.search(" 活动 ", 3, "ACTIVITY", " 社团 ", 2, 12, model);

        assertEquals("search", view);
        assertEquals(pageResult, model.getAttribute("pageResult"));
        assertEquals(List.of(thread), model.getAttribute("threads"));
        assertEquals(List.of(board), model.getAttribute("boardOptions"));
        assertEquals("活动", model.getAttribute("keyword"));
        assertEquals(3, model.getAttribute("selectedBoardId"));
        assertEquals("ACTIVITY", model.getAttribute("threadType"));
        assertEquals("社团", model.getAttribute("tag"));
        assertNotNull(model.getAttribute("threadTypeOptions"));
        verify(forumService).searchThreads(" 活动 ", 3, "ACTIVITY", " 社团 ", 2, 12);
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = SearchController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(searchController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
