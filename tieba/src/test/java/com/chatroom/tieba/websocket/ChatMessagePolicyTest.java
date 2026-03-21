package com.chatroom.tieba.websocket;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessagePolicyTest {

    @Test
    void shouldRejectSensitiveMessage() {
        ChatMessagePolicy policy = new ChatMessagePolicy(20, 200, Set.of("赌博", "违禁词"));

        ChatMessagePolicy.ValidationResult result = policy.validate(1L, "这是一个赌博网站");

        assertFalse(result.isPassed());
        assertEquals("消息包含敏感词，已被拦截", result.getRejectReason());
    }

    @Test
    void shouldRejectTooLongMessage() {
        ChatMessagePolicy policy = new ChatMessagePolicy(20, 5, Set.of());

        ChatMessagePolicy.ValidationResult result = policy.validate(1L, "123456");

        assertFalse(result.isPassed());
        assertEquals("消息过长，最多允许 5 个字符", result.getRejectReason());
    }

    @Test
    void shouldRejectWhenRateLimitExceeded() {
        ChatMessagePolicy policy = new ChatMessagePolicy(2, 200, Set.of());

        ChatMessagePolicy.ValidationResult first = policy.validate(1L, "a");
        ChatMessagePolicy.ValidationResult second = policy.validate(1L, "b");
        ChatMessagePolicy.ValidationResult third = policy.validate(1L, "c");

        assertTrue(first.isPassed());
        assertTrue(second.isPassed());
        assertFalse(third.isPassed());
        assertEquals("发送过于频繁，请稍后再试", third.getRejectReason());
    }
}
