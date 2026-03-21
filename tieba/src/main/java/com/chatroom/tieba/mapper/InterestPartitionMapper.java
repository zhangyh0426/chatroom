package com.chatroom.tieba.mapper;

import com.chatroom.tieba.entity.ForumInterestPartition;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InterestPartitionMapper {
    List<ForumInterestPartition> findEnabledPartitions();
    ForumInterestPartition findByCode(@Param("partitionCode") String partitionCode);
    ForumInterestPartition findByName(@Param("partitionName") String partitionName);
    int insert(ForumInterestPartition partition);
    int updateCanonicalById(ForumInterestPartition partition);
}
