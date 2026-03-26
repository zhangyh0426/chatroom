package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import com.chatroom.tieba.entity.ForumThreadImage;
import com.chatroom.tieba.service.ForumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private ForumService forumService;

    @Mock
    private HttpSession session;

    @TempDir
    Path tempDir;

    private BoardController boardController;

    @BeforeEach
    void setUp() {
        boardController = new BoardController();
        setField("forumService", forumService);
        setField("uploadRootPath", tempDir.toString());
    }

    @Test
    void shouldRenderCreateThreadPageWithBoardOptions() {
        ForumBoard boardA = new ForumBoard();
        boardA.setId(2);
        boardA.setName("Java");
        ForumBoard boardB = new ForumBoard();
        boardB.setId(7);
        boardB.setName("Spring");
        Map<ForumCategory, List<ForumBoard>> indexData = new LinkedHashMap<>();
        indexData.put(new ForumCategory(), List.of(boardA, boardB));
        when(forumService.getIndexData()).thenReturn(indexData);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = boardController.showCreateThreadForm(7, "board", model);

        assertEquals("thread/create", view);
        assertEquals(7, model.getAttribute("selectedBoardId"));
        assertEquals("board", model.getAttribute("entrySource"));
        assertEquals("/board/7", model.getAttribute("backPath"));
        assertEquals("返回Spring吧", model.getAttribute("backLabel"));
        verify(forumService).getIndexData();
    }

    @Test
    void shouldCreateThreadAndRedirectToThreadDetail() {
        UserSessionDTO user = new UserSessionDTO(9L, "alice", "Alice", null);
        when(session.getAttribute("user")).thenReturn(user);
        when(forumService.createThread(eq(3), eq(9L), eq("标题"), eq("正文"), eq("DISCUSSION"), eq(List.of()), anyList()))
                .thenReturn(101L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = boardController.createThread(3, " 标题 ", " 正文 ", "DISCUSSION", null, "home", null, session, redirectAttributes);

        assertEquals("redirect:/thread/101", view);
        verify(forumService).createThread(3, 9L, "标题", "正文", "DISCUSSION", List.of(), List.of());
    }

    @Test
    void shouldRedirectBackToCreatePageWhenTitleIsInvalid() {
        UserSessionDTO user = new UserSessionDTO(9L, "alice", "Alice", null);
        when(session.getAttribute("user")).thenReturn(user);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = boardController.createThread(3, "   ", "正文", "DISCUSSION", null, "board", null, session, redirectAttributes);

        assertEquals("redirect:/board/post/thread?boardId=3&entrySource=board", view);
        assertEquals("标题不能为空", redirectAttributes.getFlashAttributes().get("error"));
        assertEquals("", redirectAttributes.getFlashAttributes().get("title"));
        assertEquals("正文", redirectAttributes.getFlashAttributes().get("content"));
        verify(forumService, never()).createThread(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldCreateThreadFromProfileAndCarryBackflowFlag() {
        UserSessionDTO user = new UserSessionDTO(9L, "alice", "Alice", null);
        when(session.getAttribute("user")).thenReturn(user);
        when(forumService.createThread(eq(3), eq(9L), eq("标题"), eq("正文"), eq("DISCUSSION"), eq(List.of()), anyList()))
                .thenReturn(102L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = boardController.createThread(3, "标题", "正文", "DISCUSSION", null, "profile", null, session, redirectAttributes);

        assertEquals("redirect:/thread/102?fromProfile=true", view);
        verify(forumService).createThread(3, 9L, "标题", "正文", "DISCUSSION", List.of(), List.of());
    }

    @Test
    void shouldRedirectAnonymousSubmitToLoginAndPreserveReturnTo() {
        when(session.getAttribute("user")).thenReturn(null);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = boardController.createThread(3, "标题", "正文", "DISCUSSION", null, "board", null, session, redirectAttributes);

        assertEquals("redirect:/auth/login?returnTo=%2Fboard%2Fpost%2Fthread%3FboardId%3D3%26entrySource%3Dboard", view);
        verify(forumService, never()).createThread(org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldCreateImageThreadAndPersistUploadMetadata() throws Exception {
        UserSessionDTO user = new UserSessionDTO(9L, "alice", "Alice", null);
        when(session.getAttribute("user")).thenReturn(user);
        when(forumService.createThread(eq(3), eq(9L), eq("标题"), eq("正文"), eq("ACTIVITY"), eq(List.of("社团", "招新")), anyList()))
                .thenReturn(103L);
        MultipartFile imageFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(imageFile.isEmpty()).thenReturn(false);
        when(imageFile.getOriginalFilename()).thenReturn("cover.png");
        when(imageFile.getContentType()).thenReturn("image/png");
        when(imageFile.getSize()).thenReturn((long) "binary-image".getBytes(StandardCharsets.UTF_8).length);
        when(imageFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("binary-image".getBytes(StandardCharsets.UTF_8)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = boardController.createThread(3, "标题", "正文", "ACTIVITY", "社团, 招新", "home", new MultipartFile[]{imageFile}, session, redirectAttributes);

        assertEquals("redirect:/thread/103", view);
        ArgumentCaptor<List> imageCaptor = ArgumentCaptor.forClass(List.class);
        verify(forumService).createThread(eq(3), eq(9L), eq("标题"), eq("正文"), eq("ACTIVITY"), eq(List.of("社团", "招新")), imageCaptor.capture());
        List<?> images = imageCaptor.getValue();
        assertEquals(1, images.size());
        ForumThreadImage storedImage = (ForumThreadImage) images.get(0);
        assertNotNull(storedImage);
        assertEquals("cover.png", storedImage.getOriginalName());
        assertTrue(storedImage.getFilePath().startsWith("/uploads/thread-images/9/"));
        assertTrue(Files.exists(tempDir.resolve("thread-images").resolve("9")));
    }

    @Test
    void boardTemplateShouldUseUnifiedCreateThreadPage() throws Exception {
        String template = Files.readString(
                Path.of("src/main/webapp/WEB-INF/jsp/board/board.jsp"),
                StandardCharsets.UTF_8);

        assertTrue(template.contains("<c:param name=\"entrySource\" value=\"board\" />"));
        assertTrue(template.contains("${createThreadUrl}"));
        assertFalse(template.contains("href=\"#composer\""));
        assertFalse(template.contains("action=\"${pageContext.request.contextPath}/board/post/thread\" method=\"post\""));
    }

    @Test
    void createTemplateShouldExposeSourceAwareBackNavigation() throws Exception {
        String template = Files.readString(
                Path.of("src/main/webapp/WEB-INF/jsp/thread/create.jsp"),
                StandardCharsets.UTF_8);

        assertTrue(template.contains("${backPath}"));
        assertTrue(template.contains("${backLabel}"));
        assertTrue(template.contains("name=\"entrySource\""));
        assertTrue(template.contains("name=\"imageFiles\""));
        assertTrue(template.contains("enctype=\"multipart/form-data\""));
        assertTrue(template.contains("图文帖子正式上线"));
        assertTrue(template.contains("name=\"tagNames\""));
        assertFalse(template.contains("name=\"fromProfile\""));
    }

    @Test
    void springMvcShouldAllowAnonymousCreateThreadPageAccess() throws Exception {
        String mvcConfig = Files.readString(
                Path.of("src/main/resources/spring-mvc.xml"),
                StandardCharsets.UTF_8);

        assertFalse(mvcConfig.contains("<mvc:mapping path=\"/board/post/**\"/>"));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = BoardController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(boardController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
