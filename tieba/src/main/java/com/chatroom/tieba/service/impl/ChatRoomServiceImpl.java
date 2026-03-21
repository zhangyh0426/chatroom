package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumChatMember;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.mapper.ChatMemberMapper;
import com.chatroom.tieba.mapper.ChatMessageMapper;
import com.chatroom.tieba.mapper.ChatRoomMapper;
import com.chatroom.tieba.mapper.InterestPartitionMapper;
import com.chatroom.tieba.service.ChatRoomService;
import com.chatroom.tieba.service.InterestPartitionService;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.chatroom.tieba.vo.ChatRoomVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomServiceImpl.class);

    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final String DEFAULT_LOBBY_PARTITION_CODE = "SQUARE";
    private static final String DEFAULT_LOBBY_PARTITION_NAME = "广场大厅";
    private static final int DEFAULT_LOBBY_PARTITION_SORT_ORDER = 10;
    private static final String DEFAULT_GLOBAL_ROOM_CODE = "GLOBAL";
    private static final String DEFAULT_GLOBAL_ROOM_NAME = "全站大厅";
    private static final String PUBLIC_ROOM_TYPE = "PUBLIC";
    private static final String ENABLED_STATUS = "ENABLED";

    @Autowired
    private ChatRoomMapper chatRoomMapper;

    @Autowired
    private InterestPartitionMapper interestPartitionMapper;

    @Autowired
    private ChatMemberMapper chatMemberMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private InterestPartitionService interestPartitionService;

    @Autowired
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    @Override
    @Transactional
    public void ensureDefaultGlobalRoomReady() {
        try {
            ForumInterestPartition partition = ensureDefaultLobbyPartition();
            ensureDefaultGlobalRoom(partition);
        } catch (RuntimeException ex) {
            log.error("event=global_lobby_repair_failed alertLevel=HIGH partitionCode={} roomCode={} message={}",
                    DEFAULT_LOBBY_PARTITION_CODE, DEFAULT_GLOBAL_ROOM_CODE, ex.getMessage(), ex);
            throw new RuntimeException("全站大厅初始化失败", ex);
        }
    }

    @Override
    public List<ChatRoomVO> getRoomList(Long userId) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            return chatRoomMapper.findVisibleLegacyRooms();
        }
        return chatRoomMapper.findVisibleRoomsByUserId(userId);
    }

    @Override
    @Transactional
    public void createRoom(String partitionCode, String roomCode, String roomName) {
        String normalizedPartitionCode = normalizePartitionCode(partitionCode);
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        String normalizedRoomName = normalizeRoomName(roomName);
        ForumInterestPartition partition = interestPartitionService.getByCode(normalizedPartitionCode);
        if (findRoomByCodeCompat(normalizedRoomCode) != null) {
            throw new RuntimeException("群组编码已存在");
        }
        if (findRoomByNameCompat(normalizedRoomName) != null) {
            throw new RuntimeException("群组名称已存在");
        }
        ForumChatRoom room = new ForumChatRoom();
        room.setPartitionId(partition.getId());
        room.setRoomCode(normalizedRoomCode);
        room.setRoomName(normalizedRoomName);
        room.setRoomType("PUBLIC");
        room.setStatus("ENABLED");
        try {
            insertRoomCompat(room);
            ForumChatRoom created = findRoomByIdCompat(room.getId());
            if (created == null || created.getPartitionId() == null || !created.getPartitionId().equals(partition.getId())) {
                throw new RuntimeException("群组分区归属校验失败");
            }
        } catch (DataAccessException ex) {
            throw new RuntimeException("群组创建失败，请稍后重试");
        }
    }

    @Override
    public ForumChatRoom getRoomByCode(String roomCode) {
        ForumChatRoom room = findRoomByCodeCompat(roomCode);
        if (room == null) {
            throw new RuntimeException("群组不存在");
        }
        if (!"ENABLED".equalsIgnoreCase(room.getStatus())) {
            throw new RuntimeException("群组暂不可用");
        }
        return room;
    }

    @Override
    public boolean hasJoined(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            return false;
        }
        if (!avatarSchemaStartupChecker.isChatMemberTableReady()) {
            return true;
        }
        return chatMemberMapper.findByRoomIdAndUserId(roomId, userId) != null;
    }

    @Override
    @Transactional
    public boolean joinRoom(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            throw new RuntimeException("参数错误");
        }
        ForumChatRoom room = findRoomByIdCompat(roomId);
        if (room == null) {
            throw new RuntimeException("群组不存在");
        }
        if (!"ENABLED".equalsIgnoreCase(room.getStatus())) {
            throw new RuntimeException("群组暂不可加入");
        }
        if (!avatarSchemaStartupChecker.isChatMemberTableReady()) {
            return false;
        }
        ForumChatMember member = new ForumChatMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        try {
            return chatMemberMapper.insertIgnoreOrInsert(member) > 0;
        } catch (DataAccessException ex) {
            throw new RuntimeException("加入群组失败，请稍后重试");
        }
    }

    @Override
    public List<ChatMessageVO> getRecentMessages(String roomCode, int limit) {
        ForumChatRoom room = getRoomByCode(roomCode);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        try {
            return chatMessageMapper.findRecentMessagesByRoomId(room.getId(), safeLimit);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return chatMessageMapper.findRecentMessagesByRoomIdByAvatar(room.getId(), safeLimit);
            }
            throw ex;
        }
    }

    private boolean isUnknownColumn(Throwable ex, String columnName) {
        String normalizedColumnName = columnName.toLowerCase();
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("unknown column") && lowerMessage.contains(normalizedColumnName)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizePartitionCode(String partitionCode) {
        if (partitionCode == null || partitionCode.isBlank()) {
            throw new RuntimeException("分区编码不能为空");
        }
        return partitionCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RuntimeException("群组编码不能为空");
        }
        String normalized = roomCode.trim().toUpperCase(Locale.ROOT);
        if (!ROOM_CODE_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("群组编码格式不正确");
        }
        return normalized;
    }

    private String normalizeRoomName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new RuntimeException("群组名称不能为空");
        }
        String normalized = roomName.trim();
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw new RuntimeException("群组名称长度需在2到100个字符");
        }
        return normalized;
    }

    private ForumInterestPartition ensureDefaultLobbyPartition() {
        ForumInterestPartition partition = interestPartitionMapper.findByCode(DEFAULT_LOBBY_PARTITION_CODE);
        if (partition == null) {
            partition = interestPartitionMapper.findByName(DEFAULT_LOBBY_PARTITION_NAME);
        }
        if (partition == null) {
            partition = new ForumInterestPartition();
            partition.setPartitionCode(DEFAULT_LOBBY_PARTITION_CODE);
            partition.setPartitionName(DEFAULT_LOBBY_PARTITION_NAME);
            partition.setSortOrder(DEFAULT_LOBBY_PARTITION_SORT_ORDER);
            partition.setStatus(ENABLED_STATUS);
            interestPartitionMapper.insert(partition);
            if (partition.getId() == null) {
                partition = interestPartitionMapper.findByCode(DEFAULT_LOBBY_PARTITION_CODE);
            }
            if (partition == null || partition.getId() == null) {
                throw new RuntimeException("全站大厅分区创建后未能读取");
            }
            log.info("event=global_lobby_partition_created partitionCode={} partitionName={}",
                    DEFAULT_LOBBY_PARTITION_CODE, DEFAULT_LOBBY_PARTITION_NAME);
            return partition;
        }

        if (shouldUpdatePartition(partition)) {
            partition.setPartitionCode(DEFAULT_LOBBY_PARTITION_CODE);
            partition.setPartitionName(DEFAULT_LOBBY_PARTITION_NAME);
            partition.setSortOrder(DEFAULT_LOBBY_PARTITION_SORT_ORDER);
            partition.setStatus(ENABLED_STATUS);
            if (interestPartitionMapper.updateCanonicalById(partition) <= 0) {
                throw new RuntimeException("全站大厅分区修复失败");
            }
            log.info("event=global_lobby_partition_repaired partitionId={} partitionCode={}",
                    partition.getId(), DEFAULT_LOBBY_PARTITION_CODE);
        }
        return partition;
    }

    private void ensureDefaultGlobalRoom(ForumInterestPartition partition) {
        ForumChatRoom room = findRoomByCodeCompat(DEFAULT_GLOBAL_ROOM_CODE);
        if (room == null) {
            room = findRoomByNameCompat(DEFAULT_GLOBAL_ROOM_NAME);
        }
        if (room == null) {
            room = new ForumChatRoom();
            room.setPartitionId(partition.getId());
            room.setRoomCode(DEFAULT_GLOBAL_ROOM_CODE);
            room.setRoomName(DEFAULT_GLOBAL_ROOM_NAME);
            room.setRoomType(PUBLIC_ROOM_TYPE);
            room.setStatus(ENABLED_STATUS);
            insertRoomCompat(room);
            if (room.getId() == null) {
                room = findRoomByCodeCompat(DEFAULT_GLOBAL_ROOM_CODE);
            }
            if (room == null || room.getId() == null) {
                throw new RuntimeException("全站大厅创建后未能读取");
            }
            log.info("event=global_lobby_room_created roomCode={} partitionId={}",
                    DEFAULT_GLOBAL_ROOM_CODE, partition.getId());
            return;
        }

        if (shouldUpdateRoom(room, partition.getId())) {
            room.setPartitionId(partition.getId());
            room.setRoomCode(DEFAULT_GLOBAL_ROOM_CODE);
            room.setRoomName(DEFAULT_GLOBAL_ROOM_NAME);
            room.setRoomType(PUBLIC_ROOM_TYPE);
            room.setStatus(ENABLED_STATUS);
            if (updateRoomCompat(room) <= 0) {
                throw new RuntimeException("全站大厅修复失败");
            }
            log.info("event=global_lobby_room_repaired roomId={} roomCode={} partitionId={}",
                    room.getId(), DEFAULT_GLOBAL_ROOM_CODE, partition.getId());
        }
    }

    private boolean shouldUpdatePartition(ForumInterestPartition partition) {
        return !DEFAULT_LOBBY_PARTITION_CODE.equals(partition.getPartitionCode())
                || !DEFAULT_LOBBY_PARTITION_NAME.equals(partition.getPartitionName())
                || !Integer.valueOf(DEFAULT_LOBBY_PARTITION_SORT_ORDER).equals(partition.getSortOrder())
                || !ENABLED_STATUS.equalsIgnoreCase(partition.getStatus());
    }

    private boolean shouldUpdateRoom(ForumChatRoom room, Long partitionId) {
        return !partitionId.equals(room.getPartitionId())
                || !DEFAULT_GLOBAL_ROOM_CODE.equals(room.getRoomCode())
                || !DEFAULT_GLOBAL_ROOM_NAME.equals(room.getRoomName())
                || !PUBLIC_ROOM_TYPE.equalsIgnoreCase(room.getRoomType())
                || !ENABLED_STATUS.equalsIgnoreCase(room.getStatus());
    }

    private ForumChatRoom findRoomByCodeCompat(String roomCode) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            return chatRoomMapper.findByRoomCodeLegacy(roomCode);
        }
        return chatRoomMapper.findByRoomCode(roomCode);
    }

    private ForumChatRoom findRoomByNameCompat(String roomName) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            return chatRoomMapper.findByRoomNameLegacy(roomName);
        }
        return chatRoomMapper.findByRoomName(roomName);
    }

    private ForumChatRoom findRoomByIdCompat(Long roomId) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            return chatRoomMapper.findByIdLegacy(roomId);
        }
        return chatRoomMapper.findById(roomId);
    }

    private void insertRoomCompat(ForumChatRoom room) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            chatRoomMapper.insertLegacy(room);
            return;
        }
        chatRoomMapper.insert(room);
    }

    private int updateRoomCompat(ForumChatRoom room) {
        if (avatarSchemaStartupChecker.shouldPreferLegacyChatRoomSchema()) {
            return chatRoomMapper.updateCanonicalByIdLegacy(room);
        }
        return chatRoomMapper.updateCanonicalById(room);
    }
}
