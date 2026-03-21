package com.chatroom.tieba.service;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import com.chatroom.tieba.entity.ForumLikeLog;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadVO;

import java.util.List;
import java.util.Map;

public interface ForumService {
    // 首页：获取全部分类及吧
    Map<ForumCategory, List<ForumBoard>> getIndexData();
    
    // 吧内数据：吧详情及帖子列表
    ForumBoard getBoardById(Integer boardId);
    List<ThreadVO> getThreadsByBoard(Integer boardId);
    PageResult<ThreadVO> getThreadsByBoardPaged(Integer boardId, String keyword, int pageNum, int pageSize);
    PageResult<ThreadVO> searchThreads(String keyword, int pageNum, int pageSize);
    List<ThreadVO> getHotThreads(int limit);
    
    // 帖子详情页面数据：主题、增加浏览量、各楼层及其楼中楼
    ThreadVO getThreadWithViewCountInc(Long threadId);
    List<PostVO> getPostsWithReplies(Long threadId);
    
    // 核心交互：发帖
    Long createThread(Integer boardId, Long userId, String title, String content);
    
    // 核心交互：回帖（楼层盖楼）
    void createPost(Long threadId, Long userId, String content);
    
    // 核心交互：楼中楼评论小楼层
    void createReply(Long threadId, Long postId, Long userId, Long replyToUserId, String content);

    boolean deleteThreadByAuthor(Long threadId, Long userId);
    ForumLikeLog likeThread(Long threadId, Long userId);
    PageResult<ThreadVO> getThreadsByUser(Long userId, int pageNum, int pageSize);
    PageResult<PostVO> getPostsByUser(Long userId, int pageNum, int pageSize);
}
