package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.mapper.InterestPartitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
