package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.UserAccount;
import org.apache.ibatis.annotations.Param;

public interface UserAccountMapper {
    int insert(UserAccount account);
    UserAccount findByUsername(@Param("username") String username);
}