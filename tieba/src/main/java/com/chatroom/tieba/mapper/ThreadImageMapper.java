package com.chatroom.tieba.mapper;

import com.chatroom.tieba.vo.ThreadImageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ThreadImageMapper {
    int insert(com.chatroom.tieba.entity.ForumThreadImage image);
    List<ThreadImageVO> findByThreadId(@Param("threadId") Long threadId);
}
