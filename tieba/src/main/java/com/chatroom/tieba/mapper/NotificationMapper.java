package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumNotification;
import com.chatroom.tieba.vo.NotificationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationMapper {
    int insert(ForumNotification notification);
    List<NotificationVO> findByUserId(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);
    List<NotificationVO> findByUserIdByAvatar(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("size") int size);
    int countByUserId(@Param("userId") Long userId);
    int countUnreadByUserId(@Param("userId") Long userId);
    int markAllReadByUserId(@Param("userId") Long userId);
}
