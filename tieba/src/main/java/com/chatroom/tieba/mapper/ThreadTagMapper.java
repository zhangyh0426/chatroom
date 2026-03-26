package com.chatroom.tieba.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ThreadTagMapper {
    int insert(@Param("threadId") Long threadId, @Param("tagId") Long tagId);
    List<Long> findThreadIdsByNormalizedTag(@Param("normalizedName") String normalizedName);
}
