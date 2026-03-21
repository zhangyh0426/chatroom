package com.chatroom.tieba.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatMessagePolicy {

    private final int rateLimitPerMinute;
    private final int maxLength;
    private final Set<String> sensitiveWords;
    private final ConcurrentHashMap<Long, UserRateWindow> userWindows = new ConcurrentHashMap<>();

    public ChatMessagePolicy(int rateLimitPerMinute, int maxLength, Set<String> sensitiveWords) {
        this.rateLimitPerMinute = Math.max(rateLimitPerMinute, 1);
        this.maxLength = Math.max(maxLength, 1);
        this.sensitiveWords = sensitiveWords == null ? Collections.emptySet() : sensitiveWords.stream()
                .filter(word -> word != null && !word.isBlank())
                .map(word -> word.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public ValidationResult validate(Long userId, String content) {
        if (userId == null) {
            return ValidationResult.reject("用户身份无效");
        }
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            return ValidationResult.reject("消息不能为空");
        }
        if (normalized.length() > maxLength) {
            return ValidationResult.reject("消息过长，最多允许 " + maxLength + " 个字符");
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        for (String sensitiveWord : sensitiveWords) {
            if (!sensitiveWord.isEmpty() && lowered.contains(sensitiveWord)) {
                return ValidationResult.reject("消息包含敏感词，已被拦截");
            }
        }
        if (!consumeRateQuota(userId)) {
            return ValidationResult.reject("发送过于频繁，请稍后再试");
        }
        return ValidationResult.pass(normalized);
    }

    private boolean consumeRateQuota(Long userId) {
        UserRateWindow window = userWindows.computeIfAbsent(userId, key -> new UserRateWindow());
        synchronized (window) {
            Instant now = Instant.now();
            if (Duration.between(window.windowStart, now).toMinutes() >= 1) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= rateLimitPerMinute) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    public static final class ValidationResult {
        private final boolean passed;
        private final String content;
        private final String rejectReason;

        private ValidationResult(boolean passed, String content, String rejectReason) {
            this.passed = passed;
            this.content = content;
            this.rejectReason = rejectReason;
        }

        public static ValidationResult pass(String content) {
            return new ValidationResult(true, content, null);
        }

        public static ValidationResult reject(String reason) {
            return new ValidationResult(false, null, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getContent() {
            return content;
        }

        public String getRejectReason() {
            return rejectReason;
        }
    }

    private static final class UserRateWindow {
        private Instant windowStart = Instant.now();
        private int count = 0;
    }
}
