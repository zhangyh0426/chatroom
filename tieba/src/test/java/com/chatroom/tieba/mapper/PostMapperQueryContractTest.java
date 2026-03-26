package com.chatroom.tieba.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostMapperQueryContractTest {

    @Test
    void myReplyQueriesShouldExcludeMainFloorPosts() throws Exception {
        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/PostMapper.xml"),
                StandardCharsets.UTF_8);

        int occurrences = mapperXml.split("AND p.floor_no > 1", -1).length - 1;
        assertEquals(3, occurrences);
    }
}
