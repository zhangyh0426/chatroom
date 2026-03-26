package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumNotification;
import com.chatroom.tieba.entity.UserProfile;
import com.chatroom.tieba.mapper.NotificationMapper;
import com.chatroom.tieba.mapper.UserProfileMapper;
import com.chatroom.tieba.service.NotificationService;
import com.chatroom.tieba.vo.NotificationVO;
import com.chatroom.tieba.vo.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\p{L}\\p{N}_\\-]{2,30})");

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    public PageResult<NotificationVO> getNotifications(Long userId, int pageNum, int pageSize) {
        if (userId == null || userId <= 0) {
            return new PageResult<>(List.of(), 1, Math.max(pageSize, 1), 0);
        }
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 50));
        int offset = (safePageNum - 1) * safePageSize;
        try {
            List<NotificationVO> notifications;
            try {
                notifications = notificationMapper.findByUserId(userId, offset, safePageSize);
            } catch (DataAccessException ex) {
                if (isUnknownColumn(ex, "avatar_path")) {
                    notifications = notificationMapper.findByUserIdByAvatar(userId, offset, safePageSize);
                } else {
                    throw ex;
                }
            }
            int totalCount = notificationMapper.countByUserId(userId);
            return new PageResult<>(notifications, safePageNum, safePageSize, totalCount);
        } catch (DataAccessException ex) {
            log.warn("event=notification_query_failed userId={} message={}", userId, rootMessage(ex), ex);
            return new PageResult<>(List.of(), safePageNum, safePageSize, 0);
        }
    }

    @Override
    public int getUnreadCount(Long userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }
        try {
            return notificationMapper.countUnreadByUserId(userId);
        } catch (DataAccessException ex) {
            log.warn("event=notification_unread_count_failed userId={} message={}", userId, rootMessage(ex), ex);
            return 0;
        }
    }

    @Override
    public void markAllRead(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            notificationMapper.markAllReadByUserId(userId);
        } catch (DataAccessException ex) {
            log.warn("event=notification_mark_all_read_failed userId={} message={}", userId, rootMessage(ex), ex);
        }
    }

    @Override
    public void createSystemNotification(Long userId, String title, String content, String targetType, Long targetId, String targetUrl) {
        createNotification(userId, null, "SYSTEM", title, content, targetType, targetId, targetUrl);
    }

    @Override
    public void createInteractionNotification(Long userId,
                                              Long actorUserId,
                                              String type,
                                              String title,
                                              String content,
                                              String targetType,
                                              Long targetId,
                                              String targetUrl) {
        if (userId == null || userId <= 0) {
            return;
        }
        if (actorUserId != null && actorUserId.equals(userId)) {
            return;
        }
        createNotification(userId, actorUserId, normalizeType(type), title, content, targetType, targetId, targetUrl);
    }

    @Override
    public void createMentionNotifications(Long actorUserId,
                                           String rawContent,
                                           String targetType,
                                           Long targetId,
                                           String targetUrl,
                                           String title,
                                           Set<Long> excludedUserIds) {
        Set<String> mentionedNicknames = extractMentionNicknames(rawContent);
        if (mentionedNicknames.isEmpty()) {
            return;
        }
        try {
            List<UserProfile> users = userProfileMapper.findByNicknames(List.copyOf(mentionedNicknames));
            if (users == null || users.isEmpty()) {
                return;
            }
            Set<Long> safeExcludedUserIds = excludedUserIds == null ? Set.of() : excludedUserIds;
            for (UserProfile user : users) {
                if (user == null || user.getUserId() == null) {
                    continue;
                }
                if (actorUserId != null && actorUserId.equals(user.getUserId())) {
                    continue;
                }
                if (safeExcludedUserIds.contains(user.getUserId())) {
                    continue;
                }
                createNotification(
                        user.getUserId(),
                        actorUserId,
                        "MENTION",
                        title,
                        "有人在互动中提到了你",
                        targetType,
                        targetId,
                        targetUrl
                );
            }
        } catch (DataAccessException ex) {
            log.warn("event=notification_mention_failed actorUserId={} message={}", actorUserId, rootMessage(ex), ex);
        }
    }

    private void createNotification(Long userId,
                                    Long actorUserId,
                                    String type,
                                    String title,
                                    String content,
                                    String targetType,
                                    Long targetId,
                                    String targetUrl) {
        if (userId == null || userId <= 0) {
            return;
        }
        ForumNotification notification = new ForumNotification();
        notification.setUserId(userId);
        notification.setActorUserId(actorUserId);
        notification.setNotificationType(normalizeType(type));
        notification.setTitle(title == null || title.isBlank() ? "你有一条新的社区通知" : title.trim());
        notification.setContent(content == null || content.isBlank() ? null : content.trim());
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetUrl(targetUrl);
        notification.setIsRead(0);
        try {
            notificationMapper.insert(notification);
        } catch (DataAccessException ex) {
            log.warn("event=notification_create_failed userId={} actorUserId={} type={} message={}",
                    userId, actorUserId, type, rootMessage(ex), ex);
        }
    }

    private Set<String> extractMentionNicknames(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return Set.of();
        }
        Matcher matcher = MENTION_PATTERN.matcher(rawContent);
        Set<String> nicknames = new LinkedHashSet<>();
        while (matcher.find()) {
            String nickname = matcher.group(1);
            if (nickname != null && !nickname.isBlank()) {
                nicknames.add(nickname.trim());
            }
        }
        return nicknames;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "SYSTEM";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isUnknownColumn(Throwable throwable, String columnName) {
        return rootMessage(throwable).toLowerCase(Locale.ROOT).contains("unknown column")
                && rootMessage(throwable).toLowerCase(Locale.ROOT).contains(columnName.toLowerCase(Locale.ROOT));
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor == null ? null : cursor.getMessage();
        return message == null ? "n/a" : message;
    }
}
