package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.UserProfile;
import org.apache.ibatis.annotations.Param;

public interface UserProfileMapper {
    int insert(UserProfile profile);
    UserProfile findByUserId(@Param("userId") Long userId);

    // v1.1: 个人中心 - 更新资料
    int updateProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio);
    int updateAvatar(@Param("userId") Long userId,
                     @Param("avatarPath") String avatarPath);
}
