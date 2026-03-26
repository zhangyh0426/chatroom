package com.chatroom.tieba.service;

import com.chatroom.tieba.vo.NotificationVO;
import com.chatroom.tieba.vo.PageResult;

import java.util.Set;

public interface NotificationService {
    PageResult<NotificationVO> getNotifications(Long userId, int pageNum, int pageSize);
    int getUnreadCount(Long userId);
    void markAllRead(Long userId);
    void createSystemNotification(Long userId, String title, String content, String targetType, Long targetId, String targetUrl);
    void createInteractionNotification(Long userId,
                                       Long actorUserId,
                                       String type,
                                       String title,
                                       String content,
                                       String targetType,
                                       Long targetId,
                                       String targetUrl);
    void createMentionNotifications(Long actorUserId,
                                    String rawContent,
                                    String targetType,
                                    Long targetId,
                                    String targetUrl,
                                    String title,
                                    Set<Long> excludedUserIds);
}
