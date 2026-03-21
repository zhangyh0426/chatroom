package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumChatMember;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.mapper.ChatMemberMapper;
import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.mapper.ChatRoomMapper;
import com.chatroom.tieba.mapper.InterestPartitionMapper;
import com.chatroom.tieba.service.InterestPartitionService;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.chatroom.tieba.vo.ChatRoomVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private InterestPartitionMapper interestPartitionMapper;

    @Mock
    private ChatMemberMapper chatMemberMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private InterestPartitionService interestPartitionService;

    @Mock
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    private ChatRoomServiceImpl chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomServiceImpl();
        setField("chatRoomMapper", chatRoomMapper);
        setField("interestPartitionMapper", interestPartitionMapper);
        setField("chatMemberMapper", chatMemberMapper);
        setField("chatMessageMapper", chatMessageMapper);
        setField("interestPartitionService", interestPartitionService);
        setField("avatarSchemaStartupChecker", avatarSchemaStartupChecker);
        lenient().when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        lenient().when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(true);
    }

    @Test
    void shouldCreateDefaultGlobalLobbyWhenPartitionAndRoomMissing() {
        when(interestPartitionMapper.findByCode("SQUARE")).thenReturn(null);
        when(interestPartitionMapper.findByName("广场大厅")).thenReturn(null);
        when(chatRoomMapper.findByRoomCode("GLOBAL")).thenReturn(null);
        when(chatRoomMapper.findByRoomName("全站大厅")).thenReturn(null);
        when(interestPartitionMapper.insert(any(ForumInterestPartition.class))).thenAnswer(invocation -> {
            ForumInterestPartition partition = invocation.getArgument(0);
            partition.setId(11L);
            return 1;
        });
        when(chatRoomMapper.insert(any(ForumChatRoom.class))).thenAnswer(invocation -> {
            ForumChatRoom room = invocation.getArgument(0);
            room.setId(22L);
            return 1;
        });

        chatRoomService.ensureDefaultGlobalRoomReady();

        ArgumentCaptor<ForumInterestPartition> partitionCaptor = ArgumentCaptor.forClass(ForumInterestPartition.class);
        ArgumentCaptor<ForumChatRoom> roomCaptor = ArgumentCaptor.forClass(ForumChatRoom.class);
        verify(interestPartitionMapper).insert(partitionCaptor.capture());
        verify(chatRoomMapper).insert(roomCaptor.capture());
        assertEquals("SQUARE", partitionCaptor.getValue().getPartitionCode());
        assertEquals("广场大厅", partitionCaptor.getValue().getPartitionName());
        assertEquals(Integer.valueOf(10), partitionCaptor.getValue().getSortOrder());
        assertEquals("ENABLED", partitionCaptor.getValue().getStatus());
        assertEquals(Long.valueOf(11L), roomCaptor.getValue().getPartitionId());
        assertEquals("GLOBAL", roomCaptor.getValue().getRoomCode());
        assertEquals("全站大厅", roomCaptor.getValue().getRoomName());
        assertEquals("PUBLIC", roomCaptor.getValue().getRoomType());
        assertEquals("ENABLED", roomCaptor.getValue().getStatus());
    }

    @Test
    void shouldRepairExistingGlobalLobbyPartitionAndRoomToCanonicalState() {
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(11L);
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("旧大厅");
        partition.setSortOrder(99);
        partition.setStatus("DISABLED");
        ForumChatRoom room = new ForumChatRoom();
        room.setId(22L);
        room.setPartitionId(99L);
        room.setRoomCode("GLOBAL");
        room.setRoomName("旧群组");
        room.setRoomType("PRIVATE");
        room.setStatus("DISABLED");
        when(interestPartitionMapper.findByCode("SQUARE")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCode("GLOBAL")).thenReturn(room);
        when(interestPartitionMapper.updateCanonicalById(any(ForumInterestPartition.class))).thenReturn(1);
        when(chatRoomMapper.updateCanonicalById(any(ForumChatRoom.class))).thenReturn(1);

        chatRoomService.ensureDefaultGlobalRoomReady();

        ArgumentCaptor<ForumInterestPartition> partitionCaptor = ArgumentCaptor.forClass(ForumInterestPartition.class);
        ArgumentCaptor<ForumChatRoom> roomCaptor = ArgumentCaptor.forClass(ForumChatRoom.class);
        verify(interestPartitionMapper).updateCanonicalById(partitionCaptor.capture());
        verify(chatRoomMapper).updateCanonicalById(roomCaptor.capture());
        assertEquals("广场大厅", partitionCaptor.getValue().getPartitionName());
        assertEquals(Integer.valueOf(10), partitionCaptor.getValue().getSortOrder());
        assertEquals("ENABLED", partitionCaptor.getValue().getStatus());
        assertEquals(Long.valueOf(11L), roomCaptor.getValue().getPartitionId());
        assertEquals("全站大厅", roomCaptor.getValue().getRoomName());
        assertEquals("PUBLIC", roomCaptor.getValue().getRoomType());
        assertEquals("ENABLED", roomCaptor.getValue().getStatus());
    }

    @Test
    void shouldAdoptNamedLobbyPartitionAndRoomWhenCanonicalCodesAreMissing() {
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(11L);
        partition.setPartitionCode("PLAZA");
        partition.setPartitionName("广场大厅");
        partition.setSortOrder(3);
        partition.setStatus("ENABLED");
        ForumChatRoom room = new ForumChatRoom();
        room.setId(22L);
        room.setPartitionId(77L);
        room.setRoomCode("LOBBY");
        room.setRoomName("全站大厅");
        room.setRoomType("PUBLIC");
        room.setStatus("ENABLED");
        when(interestPartitionMapper.findByCode("SQUARE")).thenReturn(null);
        when(interestPartitionMapper.findByName("广场大厅")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCode("GLOBAL")).thenReturn(null);
        when(chatRoomMapper.findByRoomName("全站大厅")).thenReturn(room);
        when(interestPartitionMapper.updateCanonicalById(any(ForumInterestPartition.class))).thenReturn(1);
        when(chatRoomMapper.updateCanonicalById(any(ForumChatRoom.class))).thenReturn(1);

        chatRoomService.ensureDefaultGlobalRoomReady();

        ArgumentCaptor<ForumInterestPartition> partitionCaptor = ArgumentCaptor.forClass(ForumInterestPartition.class);
        ArgumentCaptor<ForumChatRoom> roomCaptor = ArgumentCaptor.forClass(ForumChatRoom.class);
        verify(interestPartitionMapper).updateCanonicalById(partitionCaptor.capture());
        verify(chatRoomMapper).updateCanonicalById(roomCaptor.capture());
        assertEquals("SQUARE", partitionCaptor.getValue().getPartitionCode());
        assertEquals("GLOBAL", roomCaptor.getValue().getRoomCode());
        assertEquals(Long.valueOf(11L), roomCaptor.getValue().getPartitionId());
    }

    @Test
    void shouldSkipWritesWhenGlobalLobbyAlreadyCanonical() {
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(11L);
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("广场大厅");
        partition.setSortOrder(10);
        partition.setStatus("ENABLED");
        ForumChatRoom room = new ForumChatRoom();
        room.setId(22L);
        room.setPartitionId(11L);
        room.setRoomCode("GLOBAL");
        room.setRoomName("全站大厅");
        room.setRoomType("PUBLIC");
        room.setStatus("ENABLED");
        when(interestPartitionMapper.findByCode("SQUARE")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCode("GLOBAL")).thenReturn(room);

        chatRoomService.ensureDefaultGlobalRoomReady();

        verify(interestPartitionMapper, never()).insert(any(ForumInterestPartition.class));
        verify(interestPartitionMapper, never()).updateCanonicalById(any(ForumInterestPartition.class));
        verify(chatRoomMapper, never()).insert(any(ForumChatRoom.class));
        verify(chatRoomMapper, never()).updateCanonicalById(any(ForumChatRoom.class));
    }

    @Test
    void shouldThrowWhenRoomNotExists() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        when(chatRoomMapper.findByRoomCode("NONE")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatRoomService.getRoomByCode("NONE"));

        assertEquals("群组不存在", ex.getMessage());
    }

    @Test
    void shouldRejectJoinWhenRoomDisabled() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setStatus("DISABLED");
        when(chatRoomMapper.findById(1L)).thenReturn(room);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatRoomService.joinRoom(1L, 2L));

        assertEquals("群组暂不可加入", ex.getMessage());
    }

    @Test
    void shouldInsertIgnoreWhenJoinRoom() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(true);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setStatus("ENABLED");
        when(chatRoomMapper.findById(1L)).thenReturn(room);
        when(chatMemberMapper.insertIgnoreOrInsert(any(ForumChatMember.class))).thenReturn(1);

        boolean joinedNow = chatRoomService.joinRoom(1L, 2L);

        verify(chatMemberMapper).insertIgnoreOrInsert(any(ForumChatMember.class));
        assertTrue(joinedNow);
    }

    @Test
    void shouldKeepJoinIdempotentWhenMemberAlreadyExists() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(true);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setStatus("ENABLED");
        when(chatRoomMapper.findById(1L)).thenReturn(room);
        when(chatMemberMapper.insertIgnoreOrInsert(any(ForumChatMember.class))).thenReturn(0);

        boolean joinedNow = chatRoomService.joinRoom(1L, 2L);

        assertFalse(joinedNow);
    }

    @Test
    void shouldWrapJoinDataAccessExceptionWithReadableMessage() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(true);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setStatus("ENABLED");
        when(chatRoomMapper.findById(1L)).thenReturn(room);
        when(chatMemberMapper.insertIgnoreOrInsert(any(ForumChatMember.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatRoomService.joinRoom(1L, 2L));

        assertEquals("加入群组失败，请稍后重试", ex.getMessage());
    }

    @Test
    void shouldReturnJoinStatus() {
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(true);
        when(chatMemberMapper.findByRoomIdAndUserId(1L, 2L)).thenReturn(new ForumChatMember());

        boolean joined = chatRoomService.hasJoined(1L, 2L);
        boolean notJoined = chatRoomService.hasJoined(null, 2L);

        assertTrue(joined);
        assertFalse(notJoined);
    }

    @Test
    void shouldFallbackToAvatarQueryWhenAvatarPathMissing() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(3L);
        room.setRoomCode("TECH");
        room.setStatus("ENABLED");
        ChatMessageVO vo = new ChatMessageVO();
        vo.setContent("ok");
        when(chatRoomMapper.findByRoomCode("TECH")).thenReturn(room);
        when(chatMessageMapper.findRecentMessagesByRoomId(3L, 50))
                .thenThrow(new InvalidDataAccessResourceUsageException("Unknown column 'u.avatar_path'"));
        when(chatMessageMapper.findRecentMessagesByRoomIdByAvatar(3L, 50)).thenReturn(List.of(vo));

        List<ChatMessageVO> history = chatRoomService.getRecentMessages("TECH", 50);

        assertEquals(1, history.size());
        assertEquals("ok", history.get(0).getContent());
    }

    @Test
    void shouldCreateRoomWithPartitionOwnership() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(10L);
        partition.setPartitionCode("TECH_FUN");
        when(interestPartitionService.getByCode("TECH_FUN")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCode("BOOK_CLUB")).thenReturn(null);
        when(chatRoomMapper.findByRoomName("读书会")).thenReturn(null);
        when(chatRoomMapper.findById(88L)).thenAnswer(invocation -> {
            ForumChatRoom created = new ForumChatRoom();
            created.setId(88L);
            created.setPartitionId(10L);
            return created;
        });

        when(chatRoomMapper.insert(any(ForumChatRoom.class))).thenAnswer(invocation -> {
            ForumChatRoom toCreate = invocation.getArgument(0);
            toCreate.setId(88L);
            return 1;
        });

        chatRoomService.createRoom("tech_fun", "book_club", "读书会");

        verify(chatRoomMapper).insert(any(ForumChatRoom.class));
        verify(chatRoomMapper).findById(88L);
    }

    @Test
    void shouldRejectRoomWhenPartitionOwnershipInvalid() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(false);
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(10L);
        partition.setPartitionCode("TECH_FUN");
        when(interestPartitionService.getByCode("TECH_FUN")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCode("BOOK_CLUB")).thenReturn(null);
        when(chatRoomMapper.findByRoomName("读书会")).thenReturn(null);
        when(chatRoomMapper.insert(any(ForumChatRoom.class))).thenAnswer(invocation -> {
            ForumChatRoom toCreate = invocation.getArgument(0);
            toCreate.setId(99L);
            return 1;
        });
        when(chatRoomMapper.findById(99L)).thenAnswer(invocation -> {
            ForumChatRoom created = new ForumChatRoom();
            created.setId(99L);
            created.setPartitionId(20L);
            return created;
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chatRoomService.createRoom("TECH_FUN", "BOOK_CLUB", "读书会"));

        assertEquals("群组分区归属校验失败", ex.getMessage());
    }

    @Test
    void shouldUseLegacyRoomSchemaWhenCheckerRequestsCompatibilityMode() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(true);
        ChatRoomVO room = new ChatRoomVO();
        room.setRoomCode("GLOBAL");
        when(chatRoomMapper.findVisibleLegacyRooms()).thenReturn(List.of(room));

        List<ChatRoomVO> rooms = chatRoomService.getRoomList(1L);

        assertEquals(1, rooms.size());
        verify(chatRoomMapper).findVisibleLegacyRooms();
        verify(chatRoomMapper, never()).findVisibleRoomsByUserId(1L);
    }

    @Test
    void shouldTreatAllUsersAsJoinedWhenMemberTableMissing() {
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(false);

        boolean joined = chatRoomService.hasJoined(10L, 20L);

        assertTrue(joined);
        verify(chatMemberMapper, never()).findByRoomIdAndUserId(10L, 20L);
    }

    @Test
    void shouldAllowLegacyOpenAccessJoinWhenMemberTableMissing() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(true);
        when(avatarSchemaStartupChecker.isChatMemberTableReady()).thenReturn(false);
        ForumChatRoom room = new ForumChatRoom();
        room.setId(1L);
        room.setStatus("ENABLED");
        when(chatRoomMapper.findByIdLegacy(1L)).thenReturn(room);

        boolean joinedNow = chatRoomService.joinRoom(1L, 2L);

        assertFalse(joinedNow);
        verify(chatMemberMapper, never()).insertIgnoreOrInsert(any(ForumChatMember.class));
    }

    @Test
    void shouldRepairGlobalLobbyUsingLegacyRoomSchema() {
        when(avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()).thenReturn(true);
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setId(11L);
        partition.setPartitionCode("SQUARE");
        partition.setPartitionName("广场大厅");
        partition.setSortOrder(10);
        partition.setStatus("ENABLED");
        ForumChatRoom room = new ForumChatRoom();
        room.setId(22L);
        room.setPartitionId(11L);
        room.setRoomCode("GLOBAL");
        room.setRoomName("全站公共聊天室");
        room.setRoomType("PUBLIC");
        room.setStatus("ENABLED");
        when(interestPartitionMapper.findByCode("SQUARE")).thenReturn(partition);
        when(chatRoomMapper.findByRoomCodeLegacy("GLOBAL")).thenReturn(room);
        when(chatRoomMapper.updateCanonicalByIdLegacy(any(ForumChatRoom.class))).thenReturn(1);

        chatRoomService.ensureDefaultGlobalRoomReady();

        ArgumentCaptor<ForumChatRoom> roomCaptor = ArgumentCaptor.forClass(ForumChatRoom.class);
        verify(chatRoomMapper).updateCanonicalByIdLegacy(roomCaptor.capture());
        assertEquals("全站大厅", roomCaptor.getValue().getRoomName());
        verify(chatRoomMapper, never()).updateCanonicalById(any(ForumChatRoom.class));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = ChatRoomServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(chatRoomService, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
