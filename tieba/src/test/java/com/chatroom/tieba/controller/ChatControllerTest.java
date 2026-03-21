package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.service.ChatRoomService;
import com.chatroom.tieba.service.InterestPartitionService;
import com.chatroom.tieba.service.impl.AvatarSchemaStartupChecker;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.chatroom.tieba.vo.ChatRoomVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
    private static final String PARTITION_TABLE_MISSING_FEEDBACK = "请先执行 sql/v1.2_interest_partition_migration.sql 后重试加载";

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private InterestPartitionService interestPartitionService;

    @Mock
    private HttpSession httpSession;

    @Mock
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController();
        setField("chatRoomService", chatRoomService);
        setField("interestPartitionService", interestPartitionService);
        setField("avatarSchemaStartupChecker", avatarSchemaStartupChecker);
        setField("chatHistoryLimit", 50);
    }

    @Test
    void shouldRenderRoomListPage() {
        UserSessionDTO user = buildUser();
        ChatRoomVO room = new ChatRoomVO();
        room.setRoomCode("GLOBAL");
        room.setPartitionCode("SQUARE");
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("广场大厅");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        when(chatRoomService.getRoomList(1L)).thenReturn(List.of(room));
        when(interestPartitionService.getEnabledPartitions()).thenReturn(List.of(partition));
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertEquals(1, ((List<?>) model.getAttribute("rooms")).size());
        assertFalse((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals(1, ((java.util.Map<?, ?>) model.getAttribute("partitionedRooms")).size());
        verify(chatRoomService).ensureDefaultGlobalRoomReady();
    }

    @Test
    void shouldRenderEmptyFeedbackWhenRoomListLoadFailed() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        when(chatRoomService.getRoomList(1L)).thenThrow(new RuntimeException("列表暂时不可用"));
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertTrue((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals("列表暂时不可用", model.getAttribute("roomsFeedback"));
        assertEquals(0, ((List<?>) model.getAttribute("rooms")).size());
        verify(chatRoomService).ensureDefaultGlobalRoomReady();
    }

    @Test
    void shouldDegradeBeforeQueryWhenCheckerReportsTableNotReady() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(false);
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertTrue((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals(PARTITION_TABLE_MISSING_FEEDBACK, model.getAttribute("roomsFeedback"));
        verify(interestPartitionService, never()).getEnabledPartitions();
        verify(chatRoomService, never()).getRoomList(1L);
    }

    @Test
    void shouldUseReadableMessageWhenPartitionTableMissingExceptionHappens() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        when(interestPartitionService.getEnabledPartitions())
                .thenThrow(new RuntimeException("Table 'tieba_local.forum_interest_partition' doesn't exist"));
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertTrue((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals(PARTITION_TABLE_MISSING_FEEDBACK, model.getAttribute("roomsFeedback"));
    }

    @Test
    void shouldUseReadableMessageWhenPartitionTableMissingFromNestedCause() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        RuntimeException sqlError = new RuntimeException("Unknown table 'tieba_local.forum_interest_partition'");
        when(chatRoomService.getRoomList(1L))
                .thenThrow(new RuntimeException("群组查询失败", sqlError));
        when(interestPartitionService.getEnabledPartitions()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertTrue((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals(PARTITION_TABLE_MISSING_FEEDBACK, model.getAttribute("roomsFeedback"));
    }

    @Test
    void shouldKeepRetryLoadEntryOnRoomsPageTemplate() throws Exception {
        String template = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/chat/rooms.jsp"), StandardCharsets.UTF_8);

        assertTrue(template.contains("${roomsFeedback}"));
        assertTrue(template.contains("alert alert-info"));
        assertTrue(template.contains(">重试加载<"));
        assertTrue(template.contains("/chat/rooms/manage"));
        assertTrue(template.contains("<a class=\"board-card interactive-card\""));
        assertTrue(template.contains("class=\"board-cta ${room.joined ? 'is-joined' : ''}\""));
        assertFalse(template.contains("先建分区再建分组"));
        assertFalse(template.contains("action=\"${pageContext.request.contextPath}/chat/rooms/create\""));
        assertFalse(template.contains("onclick=\"window.location.href"));
        assertFalse(template.contains("role=\"link\""));
        assertFalse(template.contains("tabindex=\"0\""));
    }

    @Test
    void shouldExposeCreateFormOnManagePageTemplate() throws Exception {
        String template = Files.readString(Path.of("src/main/webapp/WEB-INF/jsp/chat/manage.jsp"), StandardCharsets.UTF_8);

        assertTrue(template.contains("创建兴趣分区与群组"));
        assertTrue(template.contains("先建分区再建分组"));
        assertTrue(template.contains("action=\"${pageContext.request.contextPath}/chat/rooms/create\""));
    }

    @Test
    void shouldRecoverRenderAfterRetryWhenSchemaGetsReady() {
        UserSessionDTO user = buildUser();
        ChatRoomVO room = new ChatRoomVO();
        room.setRoomCode("GLOBAL");
        room.setPartitionCode("SQUARE");
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("广场大厅");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(false, true);
        when(chatRoomService.getRoomList(1L)).thenReturn(List.of(room));
        when(interestPartitionService.getEnabledPartitions()).thenReturn(List.of(partition));
        Model firstModel = new ExtendedModelMap();
        Model secondModel = new ExtendedModelMap();

        String firstView = chatController.chatRooms(httpSession, firstModel);
        String secondView = chatController.chatRooms(httpSession, secondModel);

        assertEquals("chat/rooms", firstView);
        assertTrue((Boolean) firstModel.getAttribute("roomsLoadFailed"));
        assertEquals("chat/rooms", secondView);
        assertFalse((Boolean) secondModel.getAttribute("roomsLoadFailed"));
        assertEquals(1, ((List<?>) secondModel.getAttribute("rooms")).size());
        verify(avatarSchemaStartupChecker, times(2)).probeInterestPartitionTableReady();
        verify(chatRoomService, times(1)).ensureDefaultGlobalRoomReady();
        verify(chatRoomService, times(1)).getRoomList(1L);
        verify(interestPartitionService, times(1)).getEnabledPartitions();
    }

    @Test
    void shouldKeepRoomListAvailableWhenGlobalLobbyRepairFails() {
        UserSessionDTO user = buildUser();
        ChatRoomVO room = new ChatRoomVO();
        room.setRoomCode("TECH");
        room.setPartitionCode("SQUARE");
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("广场大厅");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        doThrow(new RuntimeException("全站大厅初始化失败")).when(chatRoomService).ensureDefaultGlobalRoomReady();
        when(interestPartitionService.getEnabledPartitions()).thenReturn(List.of(partition));
        when(chatRoomService.getRoomList(1L)).thenReturn(List.of(room));
        Model model = new ExtendedModelMap();

        String view = chatController.chatRooms(httpSession, model);

        assertEquals("chat/rooms", view);
        assertFalse((Boolean) model.getAttribute("roomsLoadFailed"));
        assertEquals("全站大厅初始化失败，请稍后重试", model.getAttribute("error"));
        assertEquals(1, ((List<?>) model.getAttribute("rooms")).size());
    }

    @Test
    void shouldRenderRoomDetailAndReverseHistory() {
        UserSessionDTO user = buildUser();
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setRoomCode("TECH");
        room.setStatus("ENABLED");
        ChatMessageVO first = new ChatMessageVO();
        first.setContent("new");
        ChatMessageVO second = new ChatMessageVO();
        second.setContent("old");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(chatRoomService.getRoomByCode("TECH")).thenReturn(room);
        when(chatRoomService.hasJoined(1L, 1L)).thenReturn(true);
        when(chatRoomService.getRecentMessages("TECH", 50)).thenReturn(new ArrayList<>(List.of(first, second)));
        Model model = new ExtendedModelMap();

        String view = chatController.chatRoomDetail("tech", httpSession, model);

        assertEquals("chat/room", view);
        List<?> history = (List<?>) model.getAttribute("history");
        assertEquals("old", ((ChatMessageVO) history.get(0)).getContent());
        assertEquals(true, model.getAttribute("joined"));
    }

    @Test
    void shouldJoinAndRedirect() {
        UserSessionDTO user = buildUser();
        ForumChatRoom room = new ForumChatRoom();
        room.setId(5L);
        room.setRoomCode("MOVIE");
        room.setStatus("ENABLED");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(chatRoomService.getRoomByCode("MOVIE")).thenReturn(room);
        when(chatRoomService.joinRoom(5L, 1L)).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.joinChatRoom("movie", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/MOVIE", view);
        assertEquals("加入群组成功，已为你开启发言权限", redirectAttributes.getFlashAttributes().get("success"));
    }

    @Test
    void shouldKeepJoinIdempotentMessageWhenAlreadyJoined() {
        UserSessionDTO user = buildUser();
        ForumChatRoom room = new ForumChatRoom();
        room.setId(5L);
        room.setRoomCode("MOVIE");
        room.setStatus("ENABLED");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(chatRoomService.getRoomByCode("MOVIE")).thenReturn(room);
        when(chatRoomService.joinRoom(5L, 1L)).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.joinChatRoom("movie", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/MOVIE", view);
        assertEquals("你已加入该群组，可直接发言", redirectAttributes.getFlashAttributes().get("success"));
    }

    @Test
    void shouldShowJoinErrorWhenJoinFailed() {
        UserSessionDTO user = buildUser();
        ForumChatRoom room = new ForumChatRoom();
        room.setId(5L);
        room.setRoomCode("MOVIE");
        room.setStatus("ENABLED");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(chatRoomService.getRoomByCode("MOVIE")).thenReturn(room);
        when(chatRoomService.joinRoom(5L, 1L)).thenThrow(new RuntimeException("群组暂不可加入"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.joinChatRoom("movie", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/MOVIE", view);
        assertEquals("群组暂不可加入", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    void shouldCreatePartitionThenRoomAndRedirectWithSuccess() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.createChatRoom("TECH_FUN", "技术与娱乐", 20, "book_club", "读书会", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms", view);
        assertEquals("兴趣分区与分组创建成功", redirectAttributes.getFlashAttributes().get("success"));
        inOrder(interestPartitionService, chatRoomService).verify(interestPartitionService).createPartition("TECH_FUN", "技术与娱乐", 20);
        verify(chatRoomService).createRoom("TECH_FUN", "book_club", "读书会");
    }

    @Test
    void shouldRenderManagePageWithEnabledPartitions() {
        UserSessionDTO user = buildUser();
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode("TECH_FUN");
        partition.setPartitionName("技术与娱乐");
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        when(interestPartitionService.getEnabledPartitions()).thenReturn(List.of(partition));
        Model model = new ExtendedModelMap();

        String view = chatController.manageChatRooms(httpSession, model);

        assertEquals("chat/manage", view);
        assertFalse((Boolean) model.getAttribute("manageLoadFailed"));
        assertEquals(1, ((List<?>) model.getAttribute("partitions")).size());
    }

    @Test
    void shouldRenderManagePageFallbackWhenSchemaNotReady() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(false);
        Model model = new ExtendedModelMap();

        String view = chatController.manageChatRooms(httpSession, model);

        assertEquals("chat/manage", view);
        assertTrue((Boolean) model.getAttribute("manageLoadFailed"));
        assertEquals(PARTITION_TABLE_MISSING_FEEDBACK, model.getAttribute("manageFeedback"));
        assertEquals(0, ((List<?>) model.getAttribute("partitions")).size());
    }

    @Test
    void shouldBlockRoomCreationWhenCreatePartitionFailed() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        doThrow(new RuntimeException("分区排序值不能小于0")).when(interestPartitionService).createPartition("book_zone", "阅读空间", 2);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.createChatRoom("book_zone", "阅读空间", 2, "book_club", "读书会", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/manage", view);
        assertEquals("分区排序值不能小于0", redirectAttributes.getFlashAttributes().get("error"));
        assertEquals("book_zone", redirectAttributes.getFlashAttributes().get("createPartitionCode"));
        assertEquals("阅读空间", redirectAttributes.getFlashAttributes().get("createPartitionName"));
        assertEquals(2, redirectAttributes.getFlashAttributes().get("createPartitionSortOrder"));
        assertEquals("book_club", redirectAttributes.getFlashAttributes().get("createRoomCode"));
        assertEquals("读书会", redirectAttributes.getFlashAttributes().get("createRoomName"));
        verify(chatRoomService, never()).createRoom("book_zone", "book_club", "读书会");
    }

    @Test
    void shouldContinueCreateRoomWhenPartitionAlreadyExists() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        doThrow(new RuntimeException("分区编码已存在")).when(interestPartitionService).createPartition("book_zone", "阅读空间", 2);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.createChatRoom("book_zone", "阅读空间", 2, "book_club", "读书会", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms", view);
        assertEquals("兴趣分区与分组创建成功", redirectAttributes.getFlashAttributes().get("success"));
        assertFalse(redirectAttributes.getFlashAttributes().containsKey("error"));
        verify(chatRoomService).createRoom("book_zone", "book_club", "读书会");
    }

    @Test
    void shouldKeepFormWhenCreateRoomFailedAfterPartitionCreated() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        doThrow(new RuntimeException("群组编码已存在")).when(chatRoomService).createRoom("TECH_FUN", "book_club", "读书会");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.createChatRoom("TECH_FUN", "技术与娱乐", 20, "book_club", "读书会", httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/manage", view);
        assertEquals("群组编码已存在", redirectAttributes.getFlashAttributes().get("error"));
        verify(interestPartitionService).createPartition("TECH_FUN", "技术与娱乐", 20);
        verify(chatRoomService).createRoom("TECH_FUN", "book_club", "读书会");
    }

    @Test
    void shouldRedirectGlobalRouteToGlobalRoomWhenAvailable() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.globalChat(httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms/GLOBAL", view);
        assertFalse(redirectAttributes.getFlashAttributes().containsKey("error"));
        verify(chatRoomService).ensureDefaultGlobalRoomReady();
    }

    @Test
    void shouldFallbackToRoomsWhenGlobalLobbyRepairFails() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(true);
        doThrow(new RuntimeException("全站大厅初始化失败")).when(chatRoomService).ensureDefaultGlobalRoomReady();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.globalChat(httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms", view);
        assertEquals("全站大厅初始化失败，已为你打开兴趣群组列表", redirectAttributes.getFlashAttributes().get("error"));
    }

    @Test
    void shouldFallbackToRoomsWhenChatSchemaNotReadyForGlobalEntry() {
        UserSessionDTO user = buildUser();
        when(httpSession.getAttribute("user")).thenReturn(user);
        when(avatarSchemaStartupChecker.probeInterestPartitionTableReady()).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = chatController.globalChat(httpSession, redirectAttributes);

        assertEquals("redirect:/chat/rooms", view);
        verify(chatRoomService, never()).ensureDefaultGlobalRoomReady();
    }

    @Test
    void shouldThrowWhenNoLogin() {
        when(httpSession.getAttribute("user")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatController.chatRooms(httpSession, new ExtendedModelMap()));

        assertEquals("请先登录", ex.getMessage());
    }

    private UserSessionDTO buildUser() {
        UserSessionDTO user = new UserSessionDTO();
        user.setId(1L);
        user.setNickname("tester");
        return user;
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = ChatController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(chatController, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
