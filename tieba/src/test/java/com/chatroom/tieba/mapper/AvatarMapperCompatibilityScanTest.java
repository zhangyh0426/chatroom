package com.chatroom.tieba.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarMapperCompatibilityScanTest {

    private static final Pattern SELECT_BLOCK_PATTERN = Pattern.compile("<select\\s+id=\"([^\"]+)\"[\\s\\S]*?</select>");
    private static final Pattern LEGACY_AVATAR_WITHOUT_ALIAS_PATTERN = Pattern.compile("\\bu\\.avatar\\b(?!\\s+as\\s+[a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);

    @Test
    void shouldKeepAvatarAliasCompatibleForThreadPostReplyChatQueries() throws IOException {
        List<String> mapperFiles = List.of(
                "ThreadMapper.xml",
                "PostMapper.xml",
                "ReplyMapper.xml",
                "ChatMessageMapper.xml"
        );

        Path mapperDir = Paths.get("src", "main", "resources", "mapper");
        for (String fileName : mapperFiles) {
            String xml = Files.readString(mapperDir.resolve(fileName), StandardCharsets.UTF_8);
            Matcher matcher = SELECT_BLOCK_PATTERN.matcher(xml);
            while (matcher.find()) {
                String selectId = matcher.group(1);
                String selectBlock = matcher.group();
                boolean containsAvatarJoin = selectBlock.contains("forum_user_profile");
                if (!containsAvatarJoin) {
                    continue;
                }
                assertTrue(selectBlock.toLowerCase().contains("as avatar_path"),
                        () -> fileName + "#" + selectId + " 未输出 avatar_path 别名");
                assertFalse(LEGACY_AVATAR_WITHOUT_ALIAS_PATTERN.matcher(selectBlock).find(),
                        () -> fileName + "#" + selectId + " 存在未兼容别名的 avatar 引用");
            }
        }
    }
}
