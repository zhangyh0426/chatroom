package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.*;
import com.chatroom.tieba.mapper.*;
import com.chatroom.tieba.service.ForumService;
import com.chatroom.tieba.service.NotificationService;
import com.chatroom.tieba.support.ThreadInputValidator;
import com.chatroom.tieba.support.ThreadTypeCatalog;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ReplyVO;
import com.chatroom.tieba.vo.ThreadImageVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ForumServiceImpl implements ForumService {

    @Autowired private BoardMapper boardMapper;
    @Autowired private ThreadMapper threadMapper;
    @Autowired private ThreadImageMapper threadImageMapper;
    @Autowired private PostMapper postMapper;
    @Autowired private ReplyMapper replyMapper;
    @Autowired private LikeLogMapper likeLogMapper;
    @Autowired private TagMapper tagMapper;
    @Autowired private ThreadTagMapper threadTagMapper;
    @Autowired private NotificationService notificationService;

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
    public List<ForumBoard> getAllBoards() {
        return boardMapper.findAllBoards();
    }

    @Override
    public ForumBoard getBoardById(Integer boardId) {
        return boardMapper.findBoardById(boardId);
    }

    @Override
    public List<ThreadVO> getThreadsByBoard(Integer boardId) {
        List<ThreadVO> threads = findThreadsByBoardWithAvatarFallback(boardId);
        hydrateThreadDecorations(threads);
        return threads;
    }

    @Override
    public PageResult<ThreadVO> getThreadsByBoardPaged(Integer boardId, String keyword, int pageNum, int pageSize) {
        return getThreadsByBoardPaged(boardId, keyword, null, null, pageNum, pageSize);
    }

    @Override
    public PageResult<ThreadVO> getThreadsByBoardPaged(Integer boardId,
                                                       String keyword,
                                                       String threadType,
                                                       String tagName,
                                                       int pageNum,
                                                       int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedThreadType = normalizeThreadTypeFilter(threadType);
        String normalizedTagName = ThreadInputValidator.normalizeTagKey(tagName);
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list;
        int totalCount;
        if (normalizedThreadType == null && normalizedTagName == null) {
            if (normalizedKeyword == null) {
                list = findThreadsByBoardPagedWithAvatarFallback(boardId, offset, safePageSize);
                totalCount = threadMapper.countThreadsByBoardId(boardId);
            } else {
                list = findThreadsByBoardPagedWithKeywordAndAvatarFallback(boardId, normalizedKeyword, offset, safePageSize);
                totalCount = threadMapper.countThreadsByBoardIdWithKeyword(boardId, normalizedKeyword);
            }
        } else {
            list = findThreadsByBoardFiltersWithAvatarFallback(boardId, normalizedKeyword, normalizedThreadType, normalizedTagName, offset, safePageSize);
            totalCount = threadMapper.countThreadsByBoardFilters(boardId, normalizedKeyword, normalizedThreadType, normalizedTagName);
        }
        hydrateThreadDecorations(list);
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    @Override
    public PageResult<ThreadVO> searchThreads(String keyword, int pageNum, int pageSize) {
        return searchThreads(keyword, null, null, null, pageNum, pageSize);
    }

    @Override
    public PageResult<ThreadVO> searchThreads(String keyword,
                                              Integer boardId,
                                              String threadType,
                                              String tagName,
                                              int pageNum,
                                              int pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedThreadType = normalizeThreadTypeFilter(threadType);
        String normalizedTagName = ThreadInputValidator.normalizeTagKey(tagName);
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        if (normalizedKeyword == null && boardId == null && normalizedThreadType == null && normalizedTagName == null) {
            return new PageResult<>(List.of(), safePageNum, safePageSize, 0);
        }
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list;
        int totalCount;
        if (boardId == null && normalizedThreadType == null && normalizedTagName == null && normalizedKeyword != null) {
            list = searchThreadsWithAvatarFallback(normalizedKeyword, offset, safePageSize);
            totalCount = threadMapper.countSearchByKeyword(normalizedKeyword);
        } else {
            list = searchThreadsByFiltersWithAvatarFallback(normalizedKeyword, boardId, normalizedThreadType, normalizedTagName, offset, safePageSize);
            totalCount = threadMapper.countSearchByFilters(normalizedKeyword, boardId, normalizedThreadType, normalizedTagName);
        }
        hydrateThreadDecorations(list);
        return new PageResult<>(list, safePageNum, safePageSize, totalCount);
    }

    @Override
    public List<ThreadVO> getHotThreads(int limit) {
        int safeLimit = normalizeDiscoveryLimit(limit);
        List<ThreadVO> threads = findHotThreadsWithAvatarFallback(safeLimit);
        hydrateThreadDecorations(threads);
        return threads;
    }

    @Override
    public List<ThreadVO> getLatestThreads(int limit) {
        int safeLimit = normalizeDiscoveryLimit(limit);
        List<ThreadVO> threads = findLatestThreadsWithAvatarFallback(safeLimit);
        hydrateThreadDecorations(threads);
        return threads;
    }

    @Override
    public List<ThreadVO> getEssenceThreads(int limit) {
        int safeLimit = normalizeDiscoveryLimit(limit);
        List<ThreadVO> threads = findEssenceThreadsWithAvatarFallback(safeLimit);
        hydrateThreadDecorations(threads);
        return threads;
    }

    @Override
    public List<ThreadVO> getActivityThreads(int limit) {
        int safeLimit = normalizeDiscoveryLimit(limit);
        List<ThreadVO> latestThreads = getLatestThreads(Math.max(16, safeLimit * 4));
        List<ThreadVO> activityThreads = new ArrayList<>();
        for (ThreadVO latestThread : latestThreads) {
            if (!isActivityThread(latestThread)) {
                continue;
            }
            activityThreads.add(latestThread);
            if (activityThreads.size() >= safeLimit) {
                break;
            }
        }
        return activityThreads;
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
        hydrateThreadDecorations(thread);
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
    public List<ThreadImageVO> getThreadImages(Long threadId) {
        Long validatedThreadId = requireThreadId(threadId);
        if (threadImageMapper == null) {
            return List.of();
        }
        try {
            return threadImageMapper.findByThreadId(validatedThreadId);
        } catch (DataAccessException ex) {
            if (isMissingTable(ex, "forum_thread_image")) {
                return List.of();
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public Long createThread(Integer boardId, Long userId, String title, String content) {
        return createThread(boardId, userId, title, content, "DISCUSSION", List.of(), List.of());
    }

    @Override
    @Transactional
    public Long createThread(Integer boardId,
                             Long userId,
                             String title,
                             String content,
                             String threadType,
                             List<String> tagNames,
                             List<ForumThreadImage> threadImages) {
        int validatedBoardId = ThreadInputValidator.requireBoardId(boardId);
        Long validatedUserId = requireUserId(userId);
        String validatedTitle = ThreadInputValidator.requireTitle(title);
        String validatedContent = ThreadInputValidator.requireContent(content);
        String validatedThreadType = ThreadInputValidator.requireThreadType(threadType);
        List<String> normalizedTags = normalizeThreadTags(tagNames);
        ForumBoard board = boardMapper.findBoardById(validatedBoardId);
        if (board == null) {
            throw new RuntimeException("版块不存在或已不可用");
        }
        ForumThread thread = new ForumThread();
        thread.setBoardId(validatedBoardId);
        thread.setUserId(validatedUserId);
        thread.setTitle(validatedTitle);
        thread.setContent(validatedContent);
        thread.setThreadType(validatedThreadType);
        thread.setCoverImagePath(resolveCoverImagePath(threadImages));
        threadMapper.insert(thread);
        
        ForumPost post = new ForumPost();
        post.setThreadId(thread.getId());
        post.setUserId(validatedUserId);
        post.setFloorNo(1);
        post.setContent(validatedContent);
        postMapper.insert(post);

        persistThreadImages(thread.getId(), threadImages);
        bindTags(thread.getId(), normalizedTags);
        boardMapper.increaseThreadCount(validatedBoardId);
        notifySystem(
                validatedUserId,
                "帖子发布成功",
                "你发布的《" + abbreviateText(validatedTitle, 24) + "》已进入社区内容流",
                "THREAD",
                thread.getId(),
                "/thread/" + thread.getId()
        );
        return thread.getId();
    }

    @Override
    @Transactional
    public Long createPost(Long threadId, Long userId, String content) {
        Long validatedThreadId = requireThreadId(threadId);
        Long validatedUserId = requireUserId(userId);
        String validatedContent = ThreadInputValidator.requireContent(content);
        Integer maxFloor = postMapper.getMaxFloor(validatedThreadId);
        int nextFloor = (maxFloor == null ? 1 : maxFloor) + 1;

        ForumPost post = new ForumPost();
        post.setThreadId(validatedThreadId);
        post.setUserId(validatedUserId);
        post.setFloorNo(nextFloor);
        post.setContent(validatedContent);
        postMapper.insert(post);

        threadMapper.increaseReplyCount(validatedThreadId);
        threadMapper.updateLastReplyTime(validatedThreadId);
        ThreadVO thread = findThreadByIdWithAvatarFallback(validatedThreadId);
        if (thread != null) {
            boardMapper.increasePostCount(thread.getBoardId());
            notifyInteraction(
                    thread.getUserId(),
                    validatedUserId,
                    "REPLY",
                    "你的帖子收到新回复",
                    abbreviateText(validatedContent, 64),
                    "THREAD",
                    validatedThreadId,
                    buildThreadAnchor(validatedThreadId, post.getId())
            );
            notifyMentions(
                    validatedUserId,
                    validatedContent,
                    "POST",
                    post.getId(),
                    buildThreadAnchor(validatedThreadId, post.getId()),
                    "有人在帖子回复中提到了你",
                    thread.getUserId() == null ? Set.of() : Set.of(thread.getUserId())
            );
        }
        return post.getId();
    }

    @Override
    @Transactional
    public Long createReply(Long threadId, Long postId, Long userId, Long replyToUserId, String content) {
        Long validatedThreadId = requireThreadId(threadId);
        Long validatedPostId = requirePostId(postId);
        Long validatedUserId = requireUserId(userId);
        String validatedContent = ThreadInputValidator.requireContent(content);
        ForumReply reply = new ForumReply();
        reply.setThreadId(validatedThreadId);
        reply.setPostId(validatedPostId);
        reply.setUserId(validatedUserId);
        reply.setReplyToUserId(replyToUserId);
        reply.setContent(validatedContent);
        replyMapper.insert(reply);

        threadMapper.increaseReplyCount(validatedThreadId);
        threadMapper.updateLastReplyTime(validatedThreadId);
        ThreadVO thread = findThreadByIdWithAvatarFallback(validatedThreadId);
        if (thread != null) {
            boardMapper.increasePostCount(thread.getBoardId());
        }
        Set<Long> excludedUserIds = new LinkedHashSet<>();
        if (replyToUserId != null && !replyToUserId.equals(validatedUserId)) {
            notifyInteraction(
                    replyToUserId,
                    validatedUserId,
                    "REPLY",
                    "你收到一条楼中楼回复",
                    abbreviateText(validatedContent, 64),
                    "REPLY",
                    reply.getId(),
                    buildThreadAnchor(validatedThreadId, validatedPostId)
            );
            excludedUserIds.add(replyToUserId);
        }
        ForumPost targetPost = postMapper.findById(validatedPostId);
        if (targetPost != null && targetPost.getUserId() != null && !validatedUserId.equals(targetPost.getUserId()) && !excludedUserIds.contains(targetPost.getUserId())) {
            notifyInteraction(
                    targetPost.getUserId(),
                    validatedUserId,
                    "REPLY",
                    "你的楼层收到新互动",
                    abbreviateText(validatedContent, 64),
                    "POST",
                    validatedPostId,
                    buildThreadAnchor(validatedThreadId, validatedPostId)
            );
            excludedUserIds.add(targetPost.getUserId());
        }
        if (thread != null && thread.getUserId() != null && !validatedUserId.equals(thread.getUserId()) && !excludedUserIds.contains(thread.getUserId())) {
            notifyInteraction(
                    thread.getUserId(),
                    validatedUserId,
                    "REPLY",
                    "你的帖子有新的楼中楼互动",
                    abbreviateText(validatedContent, 64),
                    "THREAD",
                    validatedThreadId,
                    buildThreadAnchor(validatedThreadId, validatedPostId)
            );
            excludedUserIds.add(thread.getUserId());
        }
        notifyMentions(
                validatedUserId,
                validatedContent,
                "REPLY",
                reply.getId(),
                buildThreadAnchor(validatedThreadId, validatedPostId),
                "有人在楼中楼回复中提到了你",
                excludedUserIds
        );
        return reply.getId();
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
        notifyInteraction(
                thread.getUserId(),
                userId,
                "LIKE",
                "你的帖子收到了新点赞",
                "有人点赞了《" + abbreviateText(thread.getTitle(), 24) + "》",
                "THREAD",
                threadId,
                "/thread/" + threadId
        );
        return log;
    }

    @Override
    public PageResult<ThreadVO> getThreadsByUser(Long userId, int pageNum, int pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<ThreadVO> list = findThreadsByUserWithAvatarFallback(userId, offset, safePageSize);
        hydrateThreadDecorations(list);
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

    private List<ThreadVO> findThreadsByBoardFiltersWithAvatarFallback(Integer boardId,
                                                                       String keyword,
                                                                       String threadType,
                                                                       String normalizedTagName,
                                                                       int offset,
                                                                       int size) {
        try {
            return threadMapper.findThreadsByBoardFilters(boardId, keyword, threadType, normalizedTagName, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findThreadsByBoardFiltersByAvatar(boardId, keyword, threadType, normalizedTagName, offset, size);
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

    private List<ThreadVO> searchThreadsByFiltersWithAvatarFallback(String keyword,
                                                                    Integer boardId,
                                                                    String threadType,
                                                                    String normalizedTagName,
                                                                    int offset,
                                                                    int size) {
        try {
            return threadMapper.searchByFilters(keyword, boardId, threadType, normalizedTagName, offset, size);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.searchByFiltersByAvatar(keyword, boardId, threadType, normalizedTagName, offset, size);
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

    private List<ThreadVO> findLatestThreadsWithAvatarFallback(int limit) {
        try {
            return threadMapper.findLatestThreads(limit);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findLatestThreadsByAvatar(limit);
            }
            throw ex;
        }
    }

    private List<ThreadVO> findEssenceThreadsWithAvatarFallback(int limit) {
        try {
            return threadMapper.findEssenceThreads(limit);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex, "avatar_path")) {
                return threadMapper.findEssenceThreadsByAvatar(limit);
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

    private void persistThreadImages(Long threadId, List<ForumThreadImage> threadImages) {
        if (threadId == null || threadImages == null || threadImages.isEmpty() || threadImageMapper == null) {
            return;
        }
        for (ForumThreadImage threadImage : threadImages) {
            if (threadImage == null) {
                continue;
            }
            threadImage.setThreadId(threadId);
            threadImageMapper.insert(threadImage);
        }
    }

    private void bindTags(Long threadId, List<String> tagNames) {
        if (threadId == null || tagNames == null || tagNames.isEmpty() || tagMapper == null || threadTagMapper == null) {
            return;
        }
        for (String tagName : tagNames) {
            String normalizedTagName = ThreadInputValidator.normalizeTagKey(tagName);
            if (normalizedTagName == null) {
                continue;
            }
            try {
                ForumTag tag = tagMapper.findByNormalizedName(normalizedTagName);
                if (tag == null) {
                    tag = new ForumTag();
                    tag.setName(tagName);
                    tag.setNormalizedName(normalizedTagName);
                    try {
                        tagMapper.insert(tag);
                    } catch (DuplicateKeyException ignored) {
                        tag = tagMapper.findByNormalizedName(normalizedTagName);
                    }
                }
                if (tag != null && tag.getId() != null) {
                    threadTagMapper.insert(threadId, tag.getId());
                }
            } catch (DataAccessException ex) {
                if (isMissingTable(ex, "forum_tag") || isMissingTable(ex, "forum_thread_tag")) {
                    return;
                }
                throw ex;
            }
        }
    }

    private List<String> loadTagsForThread(Long threadId) {
        if (threadId == null || tagMapper == null) {
            return List.of();
        }
        try {
            List<ForumTag> tags = tagMapper.findByThreadId(threadId);
            if (tags == null || tags.isEmpty()) {
                return List.of();
            }
            List<String> tagNames = new ArrayList<>();
            for (ForumTag tag : tags) {
                if (tag != null && tag.getName() != null && !tag.getName().isBlank()) {
                    tagNames.add(tag.getName());
                }
            }
            return tagNames;
        } catch (DataAccessException ex) {
            if (isMissingTable(ex, "forum_tag") || isMissingTable(ex, "forum_thread_tag")) {
                return List.of();
            }
            throw ex;
        }
    }

    private void hydrateThreadDecorations(List<ThreadVO> threads) {
        if (threads == null || threads.isEmpty()) {
            return;
        }
        for (ThreadVO thread : threads) {
            hydrateThreadDecorations(thread);
        }
    }

    private void hydrateThreadDecorations(ThreadVO thread) {
        if (thread == null || thread.getId() == null) {
            return;
        }
        thread.setTagNames(loadTagsForThread(thread.getId()));
    }

    private void notifySystem(Long userId,
                              String title,
                              String content,
                              String targetType,
                              Long targetId,
                              String targetUrl) {
        if (notificationService == null) {
            return;
        }
        notificationService.createSystemNotification(userId, title, content, targetType, targetId, targetUrl);
    }

    private void notifyInteraction(Long userId,
                                   Long actorUserId,
                                   String type,
                                   String title,
                                   String content,
                                   String targetType,
                                   Long targetId,
                                   String targetUrl) {
        if (notificationService == null) {
            return;
        }
        notificationService.createInteractionNotification(userId, actorUserId, type, title, content, targetType, targetId, targetUrl);
    }

    private void notifyMentions(Long actorUserId,
                                String rawContent,
                                String targetType,
                                Long targetId,
                                String targetUrl,
                                String title,
                                Set<Long> excludedUserIds) {
        if (notificationService == null) {
            return;
        }
        notificationService.createMentionNotifications(actorUserId, rawContent, targetType, targetId, targetUrl, title, excludedUserIds);
    }

    private List<String> normalizeThreadTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueTags = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (tagName == null) {
                continue;
            }
            String normalizedTagName = tagName.trim();
            if (normalizedTagName.isEmpty()) {
                continue;
            }
            if (normalizedTagName.length() > ThreadInputValidator.TAG_MAX_LENGTH) {
                throw new RuntimeException("单个标签长度不能超过 " + ThreadInputValidator.TAG_MAX_LENGTH + " 字符");
            }
            uniqueTags.add(normalizedTagName);
        }
        if (uniqueTags.size() > ThreadInputValidator.MAX_TAG_COUNT) {
            throw new RuntimeException("最多只能设置 " + ThreadInputValidator.MAX_TAG_COUNT + " 个标签");
        }
        return new ArrayList<>(uniqueTags);
    }

    private String resolveCoverImagePath(List<ForumThreadImage> threadImages) {
        if (threadImages == null || threadImages.isEmpty()) {
            return null;
        }
        for (ForumThreadImage threadImage : threadImages) {
            if (threadImage != null && threadImage.getFilePath() != null && !threadImage.getFilePath().isBlank()) {
                return threadImage.getFilePath();
            }
        }
        return null;
    }

    private String buildThreadAnchor(Long threadId, Long postId) {
        if (postId == null) {
            return "/thread/" + threadId;
        }
        return "/thread/" + threadId + "#post-" + postId;
    }

    private boolean isActivityThread(ThreadVO thread) {
        if (thread == null) {
            return false;
        }
        if ("ACTIVITY".equalsIgnoreCase(thread.getThreadType()) || "RECRUIT".equalsIgnoreCase(thread.getThreadType())) {
            return true;
        }
        String mergedText = (safeText(thread.getTitle()) + " " + safeText(thread.getContent())).toLowerCase(Locale.ROOT);
        return mergedText.contains("活动") || mergedText.contains("招募") || mergedText.contains("报名") || mergedText.contains("社团");
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String abbreviateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private boolean isUnknownColumn(Throwable ex, String columnName) {
        String normalizedColumnName = columnName.toLowerCase();
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("unknown column") && lowerMessage.contains(normalizedColumnName)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isMissingTable(Throwable ex, String tableName) {
        String normalizedTableName = tableName.toLowerCase();
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if ((lowerMessage.contains("doesn't exist") || lowerMessage.contains("unknown table"))
                        && lowerMessage.contains(normalizedTableName)) {
                    return true;
                }
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

    private int normalizeDiscoveryLimit(int limit) {
        return Math.max(1, Math.min(limit, 20));
    }

    private Long requireThreadId(Long threadId) {
        if (threadId == null || threadId <= 0) {
            throw new RuntimeException("帖子参数不合法");
        }
        return threadId;
    }

    private Long requirePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new RuntimeException("楼层参数不合法");
        }
        return postId;
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("用户参数不合法");
        }
        return userId;
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

    private String normalizeThreadTypeFilter(String threadType) {
        if (threadType == null || threadType.isBlank()) {
            return null;
        }
        String normalized = ThreadTypeCatalog.normalize(threadType);
        if (!ThreadTypeCatalog.isSupported(normalized)) {
            throw new RuntimeException("帖子类型不合法");
        }
        return normalized;
    }
}
