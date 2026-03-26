package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumThread;
import com.chatroom.tieba.vo.ThreadVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ThreadMapper {
    int insert(ForumThread thread);
    ForumThread findRawById(@Param("id") Long id);
    ThreadVO findById(@Param("id") Long id);
    ThreadVO findByIdByAvatar(@Param("id") Long id);
    List<ThreadVO> findThreadsByBoardId(@Param("boardId") Integer boardId);
    List<ThreadVO> findThreadsByBoardIdByAvatar(@Param("boardId") Integer boardId);
    int increaseViewCount(@Param("id") Long id);
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    int increaseReplyCount(@Param("id") Long id);
    int updateLastReplyTime(@Param("id") Long id);

    List<ThreadVO> findThreadsByBoardIdPaged(@Param("boardId") Integer boardId,
                                             @Param("offset") int offset,
                                             @Param("size") int size);
    List<ThreadVO> findThreadsByBoardIdPagedByAvatar(@Param("boardId") Integer boardId,
                                                     @Param("offset") int offset,
                                                     @Param("size") int size);
    List<ThreadVO> findThreadsByBoardIdPagedWithKeyword(@Param("boardId") Integer boardId,
                                                        @Param("keyword") String keyword,
                                                        @Param("offset") int offset,
                                                        @Param("size") int size);
    List<ThreadVO> findThreadsByBoardIdPagedWithKeywordByAvatar(@Param("boardId") Integer boardId,
                                                                @Param("keyword") String keyword,
                                                                @Param("offset") int offset,
                                                                @Param("size") int size);
    List<ThreadVO> findThreadsByBoardFilters(@Param("boardId") Integer boardId,
                                             @Param("keyword") String keyword,
                                             @Param("threadType") String threadType,
                                             @Param("normalizedTagName") String normalizedTagName,
                                             @Param("offset") int offset,
                                             @Param("size") int size);
    List<ThreadVO> findThreadsByBoardFiltersByAvatar(@Param("boardId") Integer boardId,
                                                     @Param("keyword") String keyword,
                                                     @Param("threadType") String threadType,
                                                     @Param("normalizedTagName") String normalizedTagName,
                                                     @Param("offset") int offset,
                                                     @Param("size") int size);
    int countThreadsByBoardId(@Param("boardId") Integer boardId);
    int countThreadsByBoardIdWithKeyword(@Param("boardId") Integer boardId,
                                         @Param("keyword") String keyword);
    int countThreadsByBoardFilters(@Param("boardId") Integer boardId,
                                   @Param("keyword") String keyword,
                                   @Param("threadType") String threadType,
                                   @Param("normalizedTagName") String normalizedTagName);

    int increaseLikeCount(@Param("id") Long id);
    int decreaseLikeCount(@Param("id") Long id);

    List<ThreadVO> searchByKeyword(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);
    List<ThreadVO> searchByKeywordByAvatar(@Param("keyword") String keyword,
                                           @Param("offset") int offset,
                                           @Param("size") int size);
    List<ThreadVO> searchByFilters(@Param("keyword") String keyword,
                                   @Param("boardId") Integer boardId,
                                   @Param("threadType") String threadType,
                                   @Param("normalizedTagName") String normalizedTagName,
                                   @Param("offset") int offset,
                                   @Param("size") int size);
    List<ThreadVO> searchByFiltersByAvatar(@Param("keyword") String keyword,
                                           @Param("boardId") Integer boardId,
                                           @Param("threadType") String threadType,
                                           @Param("normalizedTagName") String normalizedTagName,
                                           @Param("offset") int offset,
                                           @Param("size") int size);
    int countSearchByKeyword(@Param("keyword") String keyword);
    int countSearchByFilters(@Param("keyword") String keyword,
                             @Param("boardId") Integer boardId,
                             @Param("threadType") String threadType,
                             @Param("normalizedTagName") String normalizedTagName);

    List<ThreadVO> findHotThreads(@Param("limit") int limit);
    List<ThreadVO> findHotThreadsByAvatar(@Param("limit") int limit);
    List<ThreadVO> findLatestThreads(@Param("limit") int limit);
    List<ThreadVO> findLatestThreadsByAvatar(@Param("limit") int limit);
    List<ThreadVO> findEssenceThreads(@Param("limit") int limit);
    List<ThreadVO> findEssenceThreadsByAvatar(@Param("limit") int limit);

    List<ThreadVO> findThreadsByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);
    List<ThreadVO> findThreadsByUserIdByAvatar(@Param("userId") Long userId,
                                               @Param("offset") int offset,
                                               @Param("size") int size);
    int countThreadsByUserId(@Param("userId") Long userId);
}
