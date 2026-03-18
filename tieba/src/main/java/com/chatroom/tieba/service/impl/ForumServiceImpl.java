package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.*;
import com.chatroom.tieba.mapper.*;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ReplyVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForumServiceImpl implements ForumService {

    @Autowired private BoardMapper boardMapper;
    @Autowired private ThreadMapper threadMapper;
    @Autowired private PostMapper postMapper;
    @Autowired private ReplyMapper replyMapper;

    @Override
    public Map<ForumCategory, List<ForumBoard>> getIndexData() {
        List<ForumCategory> categories = boardMapper.findAllCategories();
        Map<ForumCategory, List<ForumBoard>> map = new LinkedHashMap<>();
        for (ForumCategory category : categories) {
            List<ForumBoard> boards = boardMapper.findBoardsByCategoryId(category.getId());
            map.put(category, boards);
        }
        return map;
    }

    @Override
    public ForumBoard getBoardById(Integer boardId) {
        return boardMapper.findBoardById(boardId);
    }

    @Override
    public List<ThreadVO> getThreadsByBoard(Integer boardId) {
        return threadMapper.findThreadsByBoardId(boardId);
    }

    @Override
    @Transactional
    public ThreadVO getThreadWithViewCountInc(Long threadId) {
        threadMapper.increaseViewCount(threadId);
        return threadMapper.findById(threadId);
    }

    @Override
    public List<PostVO> getPostsWithReplies(Long threadId) {
        List<PostVO> posts = postMapper.findPostsByThreadId(threadId);
        for (PostVO post : posts) {
            List<ReplyVO> replies = replyMapper.findRepliesByPostId(post.getId());
            post.setReplies(replies);
        }
        return posts;
    }

    @Override
    @Transactional
    public Long createThread(Integer boardId, Long userId, String title, String content) {
        ForumThread thread = new ForumThread();
        thread.setBoardId(boardId);
        thread.setUserId(userId);
        thread.setTitle(title);
        thread.setContent(content);
        threadMapper.insert(thread);
        
        // 发主楼自动算作1楼
        ForumPost post = new ForumPost();
        post.setThreadId(thread.getId());
        post.setUserId(userId);
        post.setFloorNo(1);
        post.setContent(content);
        postMapper.insert(post);

        boardMapper.increaseThreadCount(boardId);
        return thread.getId();
    }

    @Override
    @Transactional
    public void createPost(Long threadId, Long userId, String content) {
        Integer maxFloor = postMapper.getMaxFloor(threadId);
        int nextFloor = (maxFloor == null ? 1 : maxFloor) + 1;

        ForumPost post = new ForumPost();
        post.setThreadId(threadId);
        post.setUserId(userId);
        post.setFloorNo(nextFloor);
        post.setContent(content);
        postMapper.insert(post);

        // 更新冗余字段
        threadMapper.increaseReplyCount(threadId);
        threadMapper.updateLastReplyTime(threadId);
        ThreadVO thread = threadMapper.findById(threadId);
        if (thread != null) {
            boardMapper.increasePostCount(thread.getBoardId());
        }
    }

    @Override
    @Transactional
    public void createReply(Long threadId, Long postId, Long userId, Long replyToUserId, String content) {
        ForumReply reply = new ForumReply();
        reply.setThreadId(threadId);
        reply.setPostId(postId);
        reply.setUserId(userId);
        reply.setReplyToUserId(replyToUserId);
        reply.setContent(content);
        replyMapper.insert(reply);

        // 楼中楼同样记作一次回复操作
        threadMapper.increaseReplyCount(threadId);
        threadMapper.updateLastReplyTime(threadId);
        ThreadVO thread = threadMapper.findById(threadId);
        if (thread != null) {
            boardMapper.increasePostCount(thread.getBoardId());
        }
    }
}