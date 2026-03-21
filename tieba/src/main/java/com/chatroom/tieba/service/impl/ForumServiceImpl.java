package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.*;
import com.chatroom.tieba.mapper.*;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ReplyVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
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
    @Autowired private LikeLogMapper likeLogMapper;

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
        return findThreadsByBoardWithAvatarFallback(boardId);
    }

    @Override
    public PageResult<ThreadVO> getThreadsByBoardPaged(Integer boardId, String keyword, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list;
        int totalCount;
        if (normalizedKeyword == null) {
            list = findThreadsByBoardPagedWithAvatarFallback(boardId, offset, safePageSize);
            totalCount = threadMapper.countThreadsByBoardId(boardId);
        } else {
            list = findThreadsByBoardPagedWithKeywordAndAvatarFallback(boardId, normalizedKeyword, offset, safePageSize);
            totalCount = threadMapper.countThreadsByBoardIdWithKeyword(boardId, normalizedKeyword);
        }
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    @Override
    public PageResult<ThreadVO> searchThreads(String keyword, int pageNum, int pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        if (normalizedKeyword == null) {
            return new PageResult<>(List.of(), safePageNum, safePageSize, 0);
        }
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list = searchThreadsWithAvatarFallback(normalizedKeyword, offset, safePageSize);
        int totalCount = threadMapper.countSearchByKeyword(normalizedKeyword);
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    @Override
    public List<ThreadVO> getHotThreads(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return findHotThreadsWithAvatarFallback(safeLimit);
    }

    @Override
    @Transactional
    public ThreadVO getThreadWithViewCountInc(Long threadId) {
        ThreadVO thread = findThreadByIdWithAvatarFallback(threadId);
        if (thread == null) {
            throw new RuntimeException("帖子不存在或已不可见");
        }
        threadMapper.increaseViewCount(threadId);
        thread.setViewCount((thread.getViewCount() == null ? 0 : thread.getViewCount()) + 1);
        return thread;
    }

    @Override
    public List<PostVO> getPostsWithReplies(Long threadId) {
        List<PostVO> posts = findPostsByThreadWithAvatarFallback(threadId);
        for (PostVO post : posts) {
            List<ReplyVO> replies = findRepliesByPostWithAvatarFallback(post.getId());
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
        ThreadVO thread = findThreadByIdWithAvatarFallback(threadId);
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
        ThreadVO thread = findThreadByIdWithAvatarFallback(threadId);
        if (thread != null) {
            boardMapper.increasePostCount(thread.getBoardId());
        }
    }

    @Override
    @Transactional
    public boolean deleteThreadByAuthor(Long threadId, Long userId) {
        ForumThread rawThread = threadMapper.findRawById(threadId);
        if (rawThread == null) {
            throw new RuntimeException("帖子不存在");
        }
        if (!userId.equals(rawThread.getUserId())) {
            throw new RuntimeException("仅作者可删除该帖子");
        }
        if (rawThread.getStatus() == null || rawThread.getStatus() != 1) {
            return false;
        }
        int affected = threadMapper.softDeleteByIdAndUserId(threadId, userId);
        if (affected == 0) {
            return false;
        }
        if (affected != 1) {
            throw new RuntimeException("删除失败，请稍后重试");
        }
        int recyclePostCount = normalizeNonNegativeCount(rawThread.getReplyCount());
        boardMapper.decreaseThreadCount(rawThread.getBoardId(), 1);
        boardMapper.decreasePostCount(rawThread.getBoardId(), recyclePostCount);
        return true;
    }

    @Override
    @Transactional
    public ForumLikeLog likeThread(Long threadId, Long userId) {
        ThreadVO thread = findThreadByIdWithAvatarFallback(threadId);
        if (thread == null) {
            throw new RuntimeException("帖子不存在或已不可见");
        }
        ForumLikeLog existed = likeLogMapper.findByUserAndTarget(userId, threadId, "THREAD");
        if (existed != null) {
            return null;
        }
        ForumLikeLog log = new ForumLikeLog();
        log.setUserId(userId);
        log.setTargetId(threadId);
        log.setTargetType("THREAD");
        try {
            likeLogMapper.insert(log);
        } catch (DuplicateKeyException ex) {
            return null;
        }
        threadMapper.increaseLikeCount(threadId);
        return log;
    }

    @Override
    public PageResult<ThreadVO> getThreadsByUser(Long userId, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list = findThreadsByUserWithAvatarFallback(userId, offset, safePageSize);
        int totalCount = threadMapper.countThreadsByUserId(userId);
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    @Override
    public PageResult<PostVO> getPostsByUser(Long userId, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<PostVO> list = findPostsByUserWithAvatarFallback(userId, offset, safePageSize);
        int totalCount = postMapper.countPostsByUserId(userId);
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    private List<ThreadVO> findThreadsByBoardWithAvatarFallback(Integer boardId) {
        try {
            return threadMapper.findThreadsByBoardId(boardId);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findThreadsByBoardIdByAvatar(boardId);
            }
            throw ex;
        }
    }

    private List<ThreadVO> findThreadsByBoardPagedWithAvatarFallback(Integer boardId, int offset, int size) {
        try {
            return threadMapper.findThreadsByBoardIdPaged(boardId, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findThreadsByBoardIdPagedByAvatar(boardId, offset, size);
            }
            throw ex;
        }
    }

    private List<ThreadVO> findThreadsByBoardPagedWithKeywordAndAvatarFallback(Integer boardId, String keyword, int offset, int size) {
        try {
            return threadMapper.findThreadsByBoardIdPagedWithKeyword(boardId, keyword, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findThreadsByBoardIdPagedWithKeywordByAvatar(boardId, keyword, offset, size);
            }
            throw ex;
        }
    }

    private ThreadVO findThreadByIdWithAvatarFallback(Long threadId) {
        try {
            return threadMapper.findById(threadId);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findByIdByAvatar(threadId);
            }
            throw ex;
        }
    }

    private List<ThreadVO> searchThreadsWithAvatarFallback(String keyword, int offset, int size) {
        try {
            return threadMapper.searchByKeyword(keyword, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.searchByKeywordByAvatar(keyword, offset, size);
            }
            throw ex;
        }
    }

    private List<ThreadVO> findHotThreadsWithAvatarFallback(int limit) {
        try {
            return threadMapper.findHotThreads(limit);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findHotThreadsByAvatar(limit);
            }
            throw ex;
        }
    }

    private List<PostVO> findPostsByThreadWithAvatarFallback(Long threadId) {
        try {
            return postMapper.findPostsByThreadId(threadId);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return postMapper.findPostsByThreadIdByAvatar(threadId);
            }
            throw ex;
        }
    }

    private List<ThreadVO> findThreadsByUserWithAvatarFallback(Long userId, int offset, int size) {
        try {
            return threadMapper.findThreadsByUserId(userId, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findThreadsByUserIdByAvatar(userId, offset, size);
            }
            throw ex;
        }
    }

    private List<PostVO> findPostsByUserWithAvatarFallback(Long userId, int offset, int size) {
        try {
            return postMapper.findPostsByUserId(userId, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return postMapper.findPostsByUserIdByAvatar(userId, offset, size);
            }
            throw ex;
        }
    }

    private List<ReplyVO> findRepliesByPostWithAvatarFallback(Long postId) {
        try {
            return replyMapper.findRepliesByPostId(postId);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return replyMapper.findRepliesByPostIdByAvatar(postId);
            }
            throw ex;
        }
    }

    private boolean isUnknownColumn(Throwable ex, String columnName) {
        String lookup = "unknown column '" + columnName + "'";
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(lookup)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private int normalizePageNum(int pageNum) {
        return Math.max(pageNum, 1);
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 50));
    }

    private int normalizeNonNegativeCount(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(value, 0);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
