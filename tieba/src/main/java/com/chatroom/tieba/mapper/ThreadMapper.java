package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumThread;
import com.chatroom.tieba.vo.ThreadVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ThreadMapper {
    int insert(ForumThread thread);
    ThreadVO findById(@Param("id") Long id);
    List<ThreadVO> findThreadsByBoardId(@Param("boardId") Integer boardId);
    int increaseViewCount(@Param("id") Long id);
    int increaseReplyCount(@Param("id") Long id);
    int updateLastReplyTime(@Param("id") Long id);

    // v1.1: 分页查询
    List<ThreadVO> findThreadsByBoardIdPaged(@Param("boardId") Integer boardId,
                                             @Param("offset") int offset,
                                             @Param("size") int size);
    int countThreadsByBoardId(@Param("boardId") Integer boardId);

    // v1.1: 点赞
    int increaseLikeCount(@Param("id") Long id);
    int decreaseLikeCount(@Param("id") Long id);

    // v1.1: 搜索
    List<ThreadVO> searchByTitle(@Param("keyword") String keyword,
                                 @Param("offset") int offset,
                                 @Param("size") int size);
    int countSearchByTitle(@Param("keyword") String keyword);

    // v1.1: 热门榜单
    List<ThreadVO> findHotThreads(@Param("limit") int limit);

    // v1.1: 我的足迹 - 我发布的帖子
    List<ThreadVO> findThreadsByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);
    int countThreadsByUserId(@Param("userId") Long userId);
}