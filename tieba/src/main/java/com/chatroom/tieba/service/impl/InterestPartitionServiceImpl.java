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
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class InterestPartitionServiceImpl implements InterestPartitionService {

    private static final Pattern PARTITION_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final String ENABLED_STATUS = "ENABLED";
    private static final String PARTITION_CODE_PREFIX = "PART_";
    private static final int DEFAULT_SORT_ORDER = 10;
    private static final int SORT_ORDER_STEP = 10;
    private static final int CODE_RETRY_LIMIT = 5;
    private static final char[] CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

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
    public ForumInterestPartition createPartition(String partitionName) {
        String normalizedName = normalizePartitionName(partitionName);
        ForumInterestPartition existing = interestPartitionMapper.findByName(normalizedName);
        if (existing != null) {
            if (!ENABLED_STATUS.equalsIgnoreCase(existing.getStatus())) {
                throw new RuntimeException("兴趣分区暂不可用");
            }
            return existing;
        }

        ForumInterestPartition partition = new ForumInterestPartition();
        partition.setPartitionName(normalizedName);
        partition.setPartitionCode(generateUniquePartitionCode());
        partition.setSortOrder(resolveNextSortOrder());
        partition.setStatus(ENABLED_STATUS);
        try {
            interestPartitionMapper.insert(partition);
        } catch (DataAccessException ex) {
            throw new RuntimeException("兴趣分区创建失败，请稍后重试");
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

    private String generateUniquePartitionCode() {
        for (int attempt = 0; attempt < CODE_RETRY_LIMIT; attempt++) {
            String candidate = buildGeneratedCode(PARTITION_CODE_PREFIX);
            if (interestPartitionMapper.findByCode(candidate) == null) {
                return candidate;
            }
        }
        throw new RuntimeException("系统生成分区编码失败，请稍后重试");
    }

    private int resolveNextSortOrder() {
        Integer currentMax = interestPartitionMapper.findMaxSortOrder();
        if (currentMax == null) {
            return DEFAULT_SORT_ORDER;
        }
        return currentMax + SORT_ORDER_STEP;
    }

    private String buildGeneratedCode(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT));
        builder.append('_');
        for (int index = 0; index < 4; index++) {
            builder.append(CODE_CHARS[ThreadLocalRandom.current().nextInt(CODE_CHARS.length)]);
        }
        return builder.toString();
    }
}
