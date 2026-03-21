package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumLikeLog;
import com.chatroom.tieba.entity.ForumThread;
import com.chatroom.tieba.mapper.BoardMapper;
import com.chatroom.tieba.mapper.LikeLogMapper;
import com.chatroom.tieba.mapper.PostMapper;
import com.chatroom.tieba.mapper.ReplyMapper;
import com.chatroom.tieba.mapper.ThreadMapper;
import com.chatroom.tieba.vo.PageResult;
import com.chatroom.tieba.vo.PostVO;
import com.chatroom.tieba.vo.ThreadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumServiceImplCoreFeatureTest {

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private ThreadMapper threadMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private ReplyMapper replyMapper;

    @Mock
    private LikeLogMapper likeLogMapper;

    private ForumServiceImpl forumService;

    @BeforeEach
    void setUp() {
        forumService = new ForumServiceImpl();
        setField("boardMapper", boardMapper);
        setField("threadMapper", threadMapper);
        setField("postMapper", postMapper);
        setField("replyMapper", replyMapper);
        setField("likeLogMapper", likeLogMapper);
    }

    @Test
    void shouldReturnBoardPagedSearchResultWhenKeywordProvided() {
        ThreadVO thread = new ThreadVO();
        thread.setId(11L);
        when(threadMapper.findThreadsByBoardIdPagedWithKeyword(3, "java", 0, 10)).thenReturn(List.of(thread));
        when(threadMapper.countThreadsByBoardIdWithKeyword(3, "java")).thenReturn(1);

        PageResult<ThreadVO> result = forumService.getThreadsByBoardPaged(3, " java ", 1, 10);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getList().size());
        assertEquals(11L, result.getList().get(0).getId());
    }

    @Test
    void shouldSearchByTitleOrContentWithPagination() {
        ThreadVO thread = new ThreadVO();
        thread.setId(21L);
        when(threadMapper.searchByKeyword("spring", 10, 10)).thenReturn(List.of(thread));
        when(threadMapper.countSearchByKeyword("spring")).thenReturn(11);

        PageResult<ThreadVO> result = forumService.searchThreads("spring", 2, 10);

        assertEquals(2, result.getPageNum());
        assertEquals(2, result.getTotalPages());
        assertEquals(11, result.getTotalCount());
        assertEquals(21L, result.getList().get(0).getId());
    }

    @Test
    void shouldPreventDuplicateThreadLike() {
        ThreadVO thread = new ThreadVO();
        thread.setId(100L);
        when(threadMapper.findById(100L)).thenReturn(thread);
        when(likeLogMapper.findByUserAndTarget(9L, 100L, "THREAD")).thenReturn(new ForumLikeLog());

        ForumLikeLog likeLog = forumService.likeThread(100L, 9L);

        assertNull(likeLog);
        verify(likeLogMapper, never()).insert(any(ForumLikeLog.class));
        verify(threadMapper, never()).increaseLikeCount(100L);
    }

    @Test
    void shouldLikeThreadOnceWhenNoLikeLogExists() {
        ThreadVO thread = new ThreadVO();
        thread.setId(100L);
        when(threadMapper.findById(100L)).thenReturn(thread);
        when(likeLogMapper.findByUserAndTarget(9L, 100L, "THREAD")).thenReturn(null);

        ForumLikeLog likeLog = forumService.likeThread(100L, 9L);

        assertNotNull(likeLog);
        verify(likeLogMapper).insert(any(ForumLikeLog.class));
        verify(threadMapper).increaseLikeCount(100L);
    }

    @Test
    void shouldHandleDuplicateLikeInsertGracefully() {
        ThreadVO thread = new ThreadVO();
        thread.setId(100L);
        when(threadMapper.findById(100L)).thenReturn(thread);
        when(likeLogMapper.findByUserAndTarget(9L, 100L, "THREAD")).thenReturn(null);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate")).when(likeLogMapper).insert(any(ForumLikeLog.class));

        ForumLikeLog likeLog = forumService.likeThread(100L, 9L);

        assertNull(likeLog);
        verify(threadMapper, never()).increaseLikeCount(100L);
    }

    @Test
    void shouldThrowWhenViewingInvisibleThread() {
        when(threadMapper.findById(777L)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> forumService.getThreadWithViewCountInc(777L));

        assertEquals("帖子不存在或已不可见", exception.getMessage());
        verify(threadMapper, never()).increaseViewCount(777L);
    }

    @Test
    void shouldSoftDeleteThreadWhenOperatorIsAuthor() {
        ForumThread rawThread = new ForumThread();
        rawThread.setId(88L);
        rawThread.setBoardId(3);
        rawThread.setReplyCount(5);
        rawThread.setUserId(9L);
        rawThread.setStatus(1);
        when(threadMapper.findRawById(88L)).thenReturn(rawThread);
        when(threadMapper.softDeleteByIdAndUserId(88L, 9L)).thenReturn(1);

        boolean deleted = forumService.deleteThreadByAuthor(88L, 9L);

        assertTrue(deleted);
        verify(threadMapper).softDeleteByIdAndUserId(88L, 9L);
        verify(boardMapper).decreaseThreadCount(3, 1);
        verify(boardMapper).decreasePostCount(3, 5);
    }

    @Test
    void shouldRejectDeleteWhenOperatorIsNotAuthor() {
        ForumThread rawThread = new ForumThread();
        rawThread.setId(88L);
        rawThread.setUserId(10L);
        rawThread.setStatus(1);
        when(threadMapper.findRawById(88L)).thenReturn(rawThread);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> forumService.deleteThreadByAuthor(88L, 9L));

        assertEquals("仅作者可删除该帖子", exception.getMessage());
        verify(threadMapper, never()).softDeleteByIdAndUserId(88L, 9L);
    }

    @Test
    void shouldSkipRecycleWhenThreadAlreadyDeleted() {
        ForumThread rawThread = new ForumThread();
        rawThread.setId(88L);
        rawThread.setBoardId(3);
        rawThread.setReplyCount(5);
        rawThread.setUserId(9L);
        rawThread.setStatus(0);
        when(threadMapper.findRawById(88L)).thenReturn(rawThread);

        boolean deleted = forumService.deleteThreadByAuthor(88L, 9L);

        assertFalse(deleted);
        verify(threadMapper, never()).softDeleteByIdAndUserId(88L, 9L);
        verify(boardMapper, never()).decreaseThreadCount(3, 1);
        verify(boardMapper, never()).decreasePostCount(3, 5);
    }

    @Test
    void shouldRecyclePostCountAsZeroWhenReplyCountIsNegative() {
        ForumThread rawThread = new ForumThread();
        rawThread.setId(88L);
        rawThread.setBoardId(3);
        rawThread.setReplyCount(-9);
        rawThread.setUserId(9L);
        rawThread.setStatus(1);
        when(threadMapper.findRawById(88L)).thenReturn(rawThread);
        when(threadMapper.softDeleteByIdAndUserId(88L, 9L)).thenReturn(1);

        forumService.deleteThreadByAuthor(88L, 9L);

        verify(boardMapper).decreaseThreadCount(3, 1);
        verify(boardMapper).decreasePostCount(3, 0);
    }

    @Test
    void shouldReturnMyFootprintsWithIndependentPagination() {
        ThreadVO myThread = new ThreadVO();
        myThread.setId(1L);
        PostVO myPost = new PostVO();
        myPost.setId(2L);
        when(threadMapper.findThreadsByUserId(5L, 0, 5)).thenReturn(List.of(myThread));
        when(threadMapper.countThreadsByUserId(5L)).thenReturn(6);
        when(postMapper.findPostsByUserId(5L, 0, 5)).thenReturn(List.of(myPost));
        when(postMapper.countPostsByUserId(5L)).thenReturn(3);

        PageResult<ThreadVO> threadPage = forumService.getThreadsByUser(5L, 1, 5);
        PageResult<PostVO> postPage = forumService.getPostsByUser(5L, 1, 5);

        assertTrue(threadPage.hasNext());
        assertFalse(postPage.hasNext());
        assertEquals(1L, threadPage.getList().get(0).getId());
        assertEquals(2L, postPage.getList().get(0).getId());
    }

    @Test
    void shouldFallbackToLegacyAvatarColumnWhenLoadingThreadsByUser() {
        ThreadVO myThread = new ThreadVO();
        myThread.setId(11L);
        when(threadMapper.findThreadsByUserId(5L, 0, 5))
                .thenThrow(new InvalidDataAccessResourceUsageException("Unknown column 'u.avatar_path' in 'field list'"));
        when(threadMapper.findThreadsByUserIdByAvatar(5L, 0, 5)).thenReturn(List.of(myThread));
        when(threadMapper.countThreadsByUserId(5L)).thenReturn(1);

        PageResult<ThreadVO> threadPage = forumService.getThreadsByUser(5L, 1, 5);

        assertEquals(1, threadPage.getTotalCount());
        assertEquals(11L, threadPage.getList().get(0).getId());
    }

    @Test
    void shouldFallbackToLegacyAvatarColumnWhenLoadingPostsByUser() {
        PostVO myPost = new PostVO();
        myPost.setId(12L);
        when(postMapper.findPostsByUserId(5L, 0, 5))
                .thenThrow(new InvalidDataAccessResourceUsageException("Unknown column 'u.avatar_path' in 'field list'"));
        when(postMapper.findPostsByUserIdByAvatar(5L, 0, 5)).thenReturn(List.of(myPost));
        when(postMapper.countPostsByUserId(5L)).thenReturn(1);

        PageResult<PostVO> postPage = forumService.getPostsByUser(5L, 1, 5);

        assertEquals(1, postPage.getTotalCount());
        assertEquals(12L, postPage.getList().get(0).getId());
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = ForumServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(forumService, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
