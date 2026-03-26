package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumPost;
import com.chatroom.tieba.vo.PostVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface PostMapper {
    int insert(ForumPost post);
    ForumPost findById(@Param("id") Long id);
    List<PostVO> findPostsByThreadId(@Param("threadId") Long threadId);
    List<PostVO> findPostsByThreadIdByAvatar(@Param("threadId") Long threadId);
    Integer getMaxFloor(@Param("threadId") Long threadId);

    // v1.1: 分页查询楼层
    List<PostVO> findPostsByThreadIdPaged(@Param("threadId") Long threadId,
                                          @Param("offset") int offset,
                                          @Param("size") int size);
    List<PostVO> findPostsByThreadIdPagedByAvatar(@Param("threadId") Long threadId,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);
    int countPostsByThreadId(@Param("threadId") Long threadId);

    // v1.1: 点赞
    int increaseLikeCount(@Param("id") Long id);
    int decreaseLikeCount(@Param("id") Long id);

    // v1.1: 我的足迹 - 我回复的帖子
    List<PostVO> findPostsByUserId(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);
    List<PostVO> findPostsByUserIdByAvatar(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);
    int countPostsByUserId(@Param("userId") Long userId);
}
