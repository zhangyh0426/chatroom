package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumReply;
import com.chatroom.tieba.vo.ReplyVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ReplyMapper {
    int insert(ForumReply reply);
    List<ReplyVO> findRepliesByPostId(@Param("postId") Long postId);
    List<ReplyVO> findRepliesByPostIdByAvatar(@Param("postId") Long postId);
}
