package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.mapper.InterestPartitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestPartitionServiceImplTest {

    @Mock
    private InterestPartitionMapper interestPartitionMapper;

    private InterestPartitionServiceImpl interestPartitionService;

    @BeforeEach
    void setUp() {
        interestPartitionService = new InterestPartitionServiceImpl();
        setField("interestPartitionMapper", interestPartitionMapper);
    }

    @Test
    void shouldRejectBlankPartitionCode() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> interestPartitionService.getByCode(" "));
        assertEquals("分区编码不能为空", ex.getMessage());
    }

    @Test
    void shouldRejectDisabledPartition() {
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode("TECH_FUN");
        partition.setStatus("DISABLED");
        when(interestPartitionMapper.findByCode("TECH_FUN")).thenReturn(partition);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> interestPartitionService.getByCode("tech_fun"));

        assertEquals("兴趣分区暂不可用", ex.getMessage());
    }

    @Test
    void shouldCreatePartitionSuccessfully() {
        when(interestPartitionMapper.findByCode("BOOK_ZONE")).thenReturn(null);
        when(interestPartitionMapper.findByName("阅读空间")).thenReturn(null);

        interestPartitionService.createPartition("book_zone", "阅读空间", 9);

        verify(interestPartitionMapper).insert(any(ForumInterestPartition.class));
    }

    @Test
    void shouldRejectDuplicatePartitionName() {
        ForumInterestPartition existed = new ForumInterestPartition();
        existed.setPartitionName("阅读空间");
        when(interestPartitionMapper.findByCode("BOOK_ZONE")).thenReturn(null);
        when(interestPartitionMapper.findByName("阅读空间")).thenReturn(existed);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> interestPartitionService.createPartition("book_zone", "阅读空间", 0));

        assertEquals("分区名称已存在", ex.getMessage());
    }

    @Test
    void shouldReuseEnabledPartitionWhenCreatingByName() {
        ForumInterestPartition existed = new ForumInterestPartition();
        existed.setId(12L);
        existed.setPartitionCode("TECH_FUN");
        existed.setPartitionName("技术与娱乐");
        existed.setStatus("ENABLED");
        when(interestPartitionMapper.findByName("技术与娱乐")).thenReturn(existed);

        ForumInterestPartition result = interestPartitionService.createPartition("技术与娱乐");

        assertEquals(Long.valueOf(12L), result.getId());
        verify(interestPartitionMapper, never()).insert(any(ForumInterestPartition.class));
    }

    @Test
    void shouldCreateGeneratedPartitionAtEndOfList() {
        when(interestPartitionMapper.findByName("阅读空间")).thenReturn(null);
        when(interestPartitionMapper.findMaxSortOrder()).thenReturn(20);
        when(interestPartitionMapper.findByCode(anyString())).thenReturn(null);
        when(interestPartitionMapper.insert(any(ForumInterestPartition.class))).thenAnswer(invocation -> {
            ForumInterestPartition partition = invocation.getArgument(0);
            partition.setId(77L);
            return 1;
        });

        ForumInterestPartition created = interestPartitionService.createPartition("阅读空间");

        ArgumentCaptor<ForumInterestPartition> captor = ArgumentCaptor.forClass(ForumInterestPartition.class);
        verify(interestPartitionMapper).insert(captor.capture());
        assertEquals(Long.valueOf(77L), created.getId());
        assertEquals("阅读空间", captor.getValue().getPartitionName());
        assertEquals(Integer.valueOf(30), captor.getValue().getSortOrder());
        assertEquals("ENABLED", captor.getValue().getStatus());
        assertTrue(captor.getValue().getPartitionCode().startsWith("PART_"));
        assertNotNull(created.getPartitionCode());
    }

    @Test
    void shouldRetryGeneratedCodeUntilUnique() {
        ForumInterestPartition collision = new ForumInterestPartition();
        collision.setPartitionCode("PART_COLLIDE");
        when(interestPartitionMapper.findByName("摄影")).thenReturn(null);
        when(interestPartitionMapper.findMaxSortOrder()).thenReturn(null);
        when(interestPartitionMapper.findByCode(anyString())).thenReturn(collision).thenReturn((ForumInterestPartition) null);
        when(interestPartitionMapper.insert(any(ForumInterestPartition.class))).thenAnswer(invocation -> {
            ForumInterestPartition partition = invocation.getArgument(0);
            partition.setId(88L);
            return 1;
        });

        ForumInterestPartition created = interestPartitionService.createPartition("摄影");

        assertEquals(Long.valueOf(88L), created.getId());
        verify(interestPartitionMapper).insert(any(ForumInterestPartition.class));
    }

    private void setField(String fieldName, Object value) {
        try {
            Field field = InterestPartitionServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(interestPartitionService, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
