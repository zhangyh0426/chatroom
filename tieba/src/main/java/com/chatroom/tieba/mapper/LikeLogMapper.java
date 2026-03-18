package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumLikeLog;
import org.apache.ibatis.annotations.Param;

public interface LikeLogMapper {
    int insert(ForumLikeLog log);
    int deleteByUserAndTarget(@Param("userId") Long userId,
                               @Param("targetId") Long targetId,
                               @Param("targetType") String targetType);
    ForumLikeLog findByUserAndTarget(@Param("userId") Long userId,
                                      @Param("targetId") Long targetId,
                                      @Param("targetType") String targetType);
}
