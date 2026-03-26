package com.chatroom.tieba.support;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ThreadInputValidator {

    public static final int TITLE_MAX_LENGTH = 100;
    public static final int CONTENT_MAX_LENGTH = 10000;
    public static final int MAX_TAG_COUNT = 3;
    public static final int TAG_MAX_LENGTH = 12;

    private ThreadInputValidator() {
    }

    public static int requireBoardId(Integer boardId) {
        if (boardId == null || boardId <= 0) {
            throw new RuntimeException("请选择有效版块");
        }
        return boardId;
    }

    public static String requireTitle(String title) {
        return normalizeRequiredText(title, "标题不能为空", "标题长度不能超过 " + TITLE_MAX_LENGTH + " 字符", TITLE_MAX_LENGTH);
    }

    public static String requireContent(String content) {
        return normalizeRequiredText(content, "正文不能为空", "正文长度不能超过 " + CONTENT_MAX_LENGTH + " 字符", CONTENT_MAX_LENGTH);
    }

    public static String requireThreadType(String threadType) {
        String normalized = ThreadTypeCatalog.normalize(threadType);
        if (!ThreadTypeCatalog.isSupported(normalized)) {
            throw new RuntimeException("帖子类型不合法");
        }
        return normalized;
    }

    public static List<String> normalizeTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }
        String normalizedRaw = rawTags.replace('，', ',').replace('#', ' ').trim();
        String[] segments = normalizedRaw.split(",");
        Set<String> uniqueTags = new LinkedHashSet<>();
        for (String segment : segments) {
            String candidate = segment == null ? "" : segment.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (candidate.length() > TAG_MAX_LENGTH) {
                throw new RuntimeException("单个标签长度不能超过 " + TAG_MAX_LENGTH + " 字符");
            }
            uniqueTags.add(candidate);
        }
        if (uniqueTags.size() > MAX_TAG_COUNT) {
            throw new RuntimeException("最多只能设置 " + MAX_TAG_COUNT + " 个标签");
        }
        return new ArrayList<>(uniqueTags);
    }

    public static String normalizeTagKey(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(tagName.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeRequiredText(String value, String emptyMessage, String tooLongMessage, int maxLength) {
        if (value == null) {
            throw new RuntimeException(emptyMessage);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException(emptyMessage);
        }
        if (normalized.length() > maxLength) {
            throw new RuntimeException(tooLongMessage);
        }
        return normalized;
    }
}
