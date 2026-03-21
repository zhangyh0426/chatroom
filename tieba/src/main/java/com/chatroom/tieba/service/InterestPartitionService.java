package com.chatroom.tieba.service;

import com.chatroom.tieba.entity.ForumInterestPartition;

import java.util.List;

public interface InterestPartitionService {
    List<ForumInterestPartition> getEnabledPartitions();
    ForumInterestPartition getByCode(String partitionCode);
    void createPartition(String partitionCode, String partitionName, Integer sortOrder);
}
