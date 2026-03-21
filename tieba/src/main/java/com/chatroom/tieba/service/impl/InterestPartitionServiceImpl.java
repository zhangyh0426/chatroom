package com.chatroom.tieba.service.impl;

import com.chatroom.tieba.entity.ForumInterestPartition;
import com.chatroom.tieba.mapper.InterestPartitionMapper;
import com.chatroom.tieba.service.InterestPartitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class InterestPartitionServiceImpl implements InterestPartitionService {

    private static final Pattern PARTITION_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");

    @Autowired
    private InterestPartitionMapper interestPartitionMapper;

    @Override
    public List<ForumInterestPartition> getEnabledPartitions() {
        return interestPartitionMapper.findEnabledPartitions();
    }

    @Override
    public ForumInterestPartition getByCode(String partitionCode) {
        ForumInterestPartition partition = interestPartitionMapper.findByCode(normalizePartitionCode(partitionCode));
        if (partition == null) {
            throw new RuntimeException("兴趣分区不存在");
        }
        if (!"ENABLED".equalsIgnoreCase(partition.getStatus())) {
            throw new RuntimeException("兴趣分区暂不可用");
        }
        return partition;
    }

    @Override
    @Transactional
    public void createPartition(String partitionCode, String partitionName, Integer sortOrder) {
        String normalizedCode = normalizePartitionCode(partitionCode);
        String normalizedName = normalizePartitionName(partitionName);
        int normalizedSortOrder = normalizeSortOrder(sortOrder);
        if (interestPartitionMapper.findByCode(normalizedCode) != null) {
            throw new RuntimeException("分区编码已存在");
        }
        if (interestPartitionMapper.findByName(normalizedName) != null) {
            throw new RuntimeException("分区名称已存在");
        }
        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionCode(normalizedCode);
        partition.setPartitionName(normalizedName);
        partition.setSortOrder(normalizedSortOrder);
        partition.setStatus("ENABLED");
        try {
            interestPartitionMapper.insert(partition);
        } catch (DataAccessException ex) {
            throw new RuntimeException("兴趣分区创建失败，请稍后重试");
        }
    }

    private String normalizePartitionCode(String partitionCode) {
        if (partitionCode == null || partitionCode.isBlank()) {
            throw new RuntimeException("分区编码不能为空");
        }
        String normalized = partitionCode.trim().toUpperCase(Locale.ROOT);
        if (!PARTITION_CODE_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("分区编码格式不正确");
        }
        return normalized;
    }

    private String normalizePartitionName(String partitionName) {
        if (partitionName == null || partitionName.isBlank()) {
            throw new RuntimeException("分区名称不能为空");
        }
        String normalized = partitionName.trim();
        if (normalized.length() < 2 || normalized.length() > 50) {
            throw new RuntimeException("分区名称长度需在2到50个字符");
        }
        return normalized;
    }

    private int normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }
        if (sortOrder < 0) {
            throw new RuntimeException("分区排序值不能小于0");
        }
        if (sortOrder > 9999) {
            throw new RuntimeException("分区排序值不能大于9999");
        }
        return sortOrder;
    }
}
