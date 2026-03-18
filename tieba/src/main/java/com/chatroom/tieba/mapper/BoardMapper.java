package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumBoard;
import com.chatroom.tieba.entity.ForumCategory;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BoardMapper {
    List<ForumCategory> findAllCategories();
    List<ForumBoard> findBoardsByCategoryId(@Param("categoryId") Integer categoryId);
    ForumBoard findBoardById(@Param("id") Integer id);
    int increaseThreadCount(@Param("id") Integer id);
    int increasePostCount(@Param("id") Integer id);
}