package com.chatroom.tieba.controller;

import com.chatroom.tieba.dto.UserSessionDTO;
import com.chatroom.tieba.entity.ForumChatRoom;
import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.service.ChatRoomService;
import com.chatroom.tieba.service.InterestPartitionService;
import com.chatroom.tieba.service.impl.AvatarSchemaStartupChecker;
import com.chatroom.tieba.vo.ChatMessageVO;
import com.chatroom.tieba.vo.ChatRoomVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private static final String GLOBAL_ROOM_CODE = "GLOBAL";
    private static final String INTEREST_PARTITION_TABLE = "forum_interest_partition";
    private static final String CHAT_ROOM_TABLE = "forum_chat_room";
    private static final String PARTITION_TABLE_MISSING_FEEDBACK = "请先执行 sql/v1.2_interest_partition_migration.sql 后重试加载";
    private static final String GLOBAL_ROOM_INIT_FAILED_FEEDBACK = "全站大厅初始化失败，已为你打开兴趣群组列表";
    private static final String GLOBAL_ROOM_REPAIR_FAILED_LIST_FEEDBACK = "全站大厅初始化失败，请稍后重试";
    private static final String CREATE_PANEL_REDIRECT = "redirect:/chat/rooms?create=1#rooms-create";
    private static final String PARTITION_MODE_EXISTING = "existing";
    private static final String PARTITION_MODE_NEW = "new";

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private InterestPartitionService interestPartitionService;

    @Autowired
    private AvatarSchemaStartupChecker avatarSchemaStartupChecker;

    @Value("${chat.history.limit:50}")
    private int chatHistoryLimit;

    @GetMapping("/rooms")
    public String chatRooms(@RequestParam(value = "create", required = false) String create,
                            HttpSession session,
                            Model model) {
        UserSessionDTO user = requireLogin(session);
        if (!avatarSchemaStartupChecker.probeInterestPartitionTableReady()) {
            applyRoomsLoadFailure(model, PARTITION_TABLE_MISSING_FEEDBACK, Collections.emptyList());
            return "chat/rooms";
        }
        List<ForumInterestPartition> partitions = Collections.emptyList();
        List<ChatRoomVO> rooms;
        try {
            chatRoomService.ensureDefaultGlobalRoomReady();
        } catch (RuntimeException ex) {
            model.addAttribute("error", GLOBAL_ROOM_REPAIR_FAILED_LIST_FEEDBACK);
        }
        try {
            partitions = interestPartitionService.getEnabledPartitions();
            rooms = chatRoomService.getRoomList(user.getId());
            model.addAttribute("partitionedRooms", buildPartitionedRooms(partitions, rooms));
        } catch (RuntimeException ex) {
            applyRoomsLoadFailure(model, resolveRoomsFeedback(ex), partitions);
            return "chat/rooms";
        }
        if (rooms == null) {
            rooms = Collections.emptyList();
        }
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomsLoadFailed", false);
        model.addAttribute("partitions", partitions == null ? Collections.emptyList() : partitions);
        populateCreateFormDefaults(model, partitions, create != null);
        return "chat/rooms";
    }

    @GetMapping("/rooms/manage")
    public String manageChatRooms(HttpSession session) {
        requireLogin(session);
        return CREATE_PANEL_REDIRECT;
    }

    @PostMapping("/rooms/create")
    public String createChatRoom(@RequestParam("partitionMode") String partitionMode,
                                 @RequestParam(value = "existingPartitionCode", required = false) String existingPartitionCode,
                                 @RequestParam(value = "newPartitionName", required = false) String newPartitionName,
                                 @RequestParam("roomName") String roomName,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        requireLogin(session);
        try {
            String partitionCode = resolvePartitionCode(partitionMode, existingPartitionCode, newPartitionName);
            chatRoomService.createRoom(partitionCode, roomName);
            redirectAttributes.addFlashAttribute("success", "兴趣群组创建成功");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "兴趣群组创建失败" : ex.getMessage());
            redirectAttributes.addFlashAttribute("showCreatePanel", true);
            redirectAttributes.addFlashAttribute("createPartitionMode", normalizePartitionMode(partitionMode));
            redirectAttributes.addFlashAttribute("createExistingPartitionCode", existingPartitionCode == null ? "" : existingPartitionCode);
            redirectAttributes.addFlashAttribute("createNewPartitionName", newPartitionName == null ? "" : newPartitionName);
            redirectAttributes.addFlashAttribute("createRoomName", roomName);
            return CREATE_PANEL_REDIRECT;
        }
        return "redirect:/chat/rooms";
    }

    @GetMapping("/rooms/{roomCode}")
    public String chatRoomDetail(@PathVariable("roomCode") String roomCode, HttpSession session, Model model) {
        UserSessionDTO user = requireLogin(session);
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        ForumChatRoom room = chatRoomService.getRoomByCode(normalizedRoomCode);
        boolean joined = chatRoomService.hasJoined(room.getId(), user.getId());
        List<ChatMessageVO> history = chatRoomService.getRecentMessages(normalizedRoomCode, chatHistoryLimit);
        Collections.reverse(history);
        model.addAttribute("room", room);
        model.addAttribute("history", history);
        model.addAttribute("joined", joined);
        return "chat/room";
    }

    @PostMapping("/rooms/{roomCode}/join")
    public String joinChatRoom(@PathVariable("roomCode") String roomCode,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UserSessionDTO user = requireLogin(session);
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        try {
            ForumChatRoom room = chatRoomService.getRoomByCode(normalizedRoomCode);
            boolean joinedNow = chatRoomService.joinRoom(room.getId(), user.getId());
            redirectAttributes.addFlashAttribute("success", joinedNow ? "加入群组成功，已为你开启发言权限" : "你已加入该群组，可直接发言");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage() == null ? "加入群组失败，请稍后重试" : ex.getMessage());
        }
        return "redirect:/chat/rooms/" + normalizedRoomCode;
    }

    @GetMapping("/global")
    public String globalChat(HttpSession session, RedirectAttributes redirectAttributes) {
        requireLogin(session);
        if (!avatarSchemaStartupChecker.probeInterestPartitionTableReady()) {
            return "redirect:/chat/rooms";
        }
        try {
            chatRoomService.ensureDefaultGlobalRoomReady();
            return "redirect:/chat/rooms/" + GLOBAL_ROOM_CODE;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", GLOBAL_ROOM_INIT_FAILED_FEEDBACK);
            return "redirect:/chat/rooms";
        }
    }

    private UserSessionDTO requireLogin(HttpSession session) {
        if (session == null) {
            throw new RuntimeException("请先登录");
        }
        UserSessionDTO user = (UserSessionDTO) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        return user;
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new RuntimeException("群组编码不能为空");
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveRoomsFeedback(RuntimeException ex) {
        if (isMissingChatSchemaTable(ex)) {
            return PARTITION_TABLE_MISSING_FEEDBACK;
        }
        return ex.getMessage() == null ? "群组列表加载失败，请稍后重试" : ex.getMessage();
    }

    private boolean isMissingChatSchemaTable(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if ((normalized.contains(INTEREST_PARTITION_TABLE) || normalized.contains(CHAT_ROOM_TABLE))
                        && (normalized.contains("doesn't exist")
                        || normalized.contains("does not exist")
                        || normalized.contains("unknown table")
                        || normalized.contains("table"))) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private void applyRoomsLoadFailure(Model model, String feedback, List<ForumInterestPartition> partitions) {
        model.addAttribute("rooms", Collections.emptyList());
        model.addAttribute("partitionedRooms", Collections.emptyMap());
        model.addAttribute("roomsLoadFailed", true);
        model.addAttribute("roomsFeedback", feedback);
        model.addAttribute("partitions", partitions == null ? Collections.emptyList() : partitions);
    }

    private Map<ForumInterestPartition, List<ChatRoomVO>> buildPartitionedRooms(List<ForumInterestPartition> partitions, List<ChatRoomVO> rooms) {
        List<ChatRoomVO> safeRooms = rooms == null ? Collections.emptyList() : rooms;
        Map<ForumInterestPartition, List<ChatRoomVO>> grouped = new LinkedHashMap<>();
        if (partitions != null) {
            for (ForumInterestPartition partition : partitions) {
                List<ChatRoomVO> partitionRooms = new ArrayList<>();
                for (ChatRoomVO room : safeRooms) {
                    if (room != null && partition.getPartitionCode() != null && partition.getPartitionCode().equals(room.getPartitionCode())) {
                        partitionRooms.add(room);
                    }
                }
                if (!partitionRooms.isEmpty()) {
                    grouped.put(partition, partitionRooms);
                }
            }
        }
        List<ChatRoomVO> unclassified = new ArrayList<>();
        for (ChatRoomVO room : safeRooms) {
            if (room == null) {
                continue;
            }
            if (room.getPartitionCode() == null) {
                unclassified.add(room);
                continue;
            }
            boolean matched = false;
            for (ForumInterestPartition partition : grouped.keySet()) {
                if (room.getPartitionCode().equals(partition.getPartitionCode())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                unclassified.add(room);
            }
        }
        if (!unclassified.isEmpty()) {
            ForumInterestPartition fallback = new ForumInterestPartition();
            fallback.setPartitionCode("UNCATEGORIZED");
            fallback.setPartitionName("未分区");
            grouped.put(fallback, unclassified);
        }
        return grouped;
    }

    private String resolvePartitionCode(String partitionMode, String existingPartitionCode, String newPartitionName) {
        String normalizedMode = normalizePartitionMode(partitionMode);
        if (PARTITION_MODE_NEW.equals(normalizedMode)) {
            return interestPartitionService.createPartition(newPartitionName).getPartitionCode();
        }
        return interestPartitionService.getByCode(existingPartitionCode).getPartitionCode();
    }

    private String normalizePartitionMode(String partitionMode) {
        if (partitionMode == null || partitionMode.isBlank()) {
            return PARTITION_MODE_EXISTING;
        }
        String normalized = partitionMode.trim().toLowerCase(Locale.ROOT);
        if (PARTITION_MODE_NEW.equals(normalized)) {
            return PARTITION_MODE_NEW;
        }
        return PARTITION_MODE_EXISTING;
    }

    private void populateCreateFormDefaults(Model model, List<ForumInterestPartition> partitions, boolean requestedOpen) {
        List<ForumInterestPartition> safePartitions = partitions == null ? Collections.emptyList() : partitions;
        boolean hasPartitions = !safePartitions.isEmpty();
        if (!model.containsAttribute("createPartitionMode")) {
            model.addAttribute("createPartitionMode", hasPartitions ? PARTITION_MODE_EXISTING : PARTITION_MODE_NEW);
        }
        if (!model.containsAttribute("createExistingPartitionCode")) {
            model.addAttribute("createExistingPartitionCode", hasPartitions ? safePartitions.get(0).getPartitionCode() : "");
        }
        if (!model.containsAttribute("createNewPartitionName")) {
            model.addAttribute("createNewPartitionName", "");
        }
        if (!model.containsAttribute("createRoomName")) {
            model.addAttribute("createRoomName", "");
        }
        if (!model.containsAttribute("showCreatePanel")) {
            model.addAttribute("showCreatePanel", requestedOpen || !hasPartitions);
        }
    }
}
