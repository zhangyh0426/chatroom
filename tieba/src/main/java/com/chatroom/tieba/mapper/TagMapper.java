package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TagMapper {
    ForumTag findByNormalizedName(@Param("normalizedName") String normalizedName);
    int insert(ForumTag tag);
    List<ForumTag> findByThreadId(@Param("threadId") Long threadId);
}
