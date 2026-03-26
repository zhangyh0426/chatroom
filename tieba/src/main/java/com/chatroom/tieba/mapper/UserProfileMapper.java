package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.UserProfile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserProfileMapper {
    int insertByAvatarPath(UserProfile profile);
    int insertByAvatar(UserProfile profile);
    UserProfile findByUserIdByAvatarPath(@Param("userId") Long userId);
    UserProfile findByUserIdByAvatar(@Param("userId") Long userId);

    int updateProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio);
    int updateAvatarByAvatarPath(@Param("userId") Long userId,
                                 @Param("avatarPath") String avatarPath);
    int updateAvatarByAvatar(@Param("userId") Long userId,
                             @Param("avatarPath") String avatarPath);
    List<UserProfile> findByNicknames(@Param("nicknames") List<String> nicknames);
}
