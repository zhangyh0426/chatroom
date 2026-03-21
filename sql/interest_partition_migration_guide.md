# forum_interest_partition 执行与排查说明

## 执行路径

基础路径（二选一）：

1. 执行 `sql/tieba_local_schema.sql`（推荐本地运行基线，库名 `tieba_local`）
2. 或执行 `sql/schema.sql`（通用结构脚本，库名 `tieba`）

增量路径：

1. 已有旧环境（仅有 `forum_chat_room` 且无 `partition_id`）执行 `sql/v1.2_interest_partition_migration.sql`
2. `v1.2_interest_partition_migration.sql` 支持重复执行，不会重复创建 `forum_interest_partition` 数据、重复加索引或重复加外键

## 幂等执行建议

推荐命令顺序（`tieba_local` 场景）：

```sql
SOURCE sql/tieba_local_schema.sql;
SOURCE sql/v1.2_interest_partition_migration.sql;
```

重复执行增量脚本时，以下行为保持幂等：

- `forum_interest_partition` 使用 `CREATE TABLE IF NOT EXISTS`
- 预置分区数据使用 `INSERT ... ON DUPLICATE KEY UPDATE`
- `forum_chat_room.partition_id` 列使用 `ADD COLUMN IF NOT EXISTS`
- 分区索引与外键通过 `information_schema` 检查后再添加

## 缺表排查 SQL

先确认当前库：

```sql
SELECT DATABASE() AS current_db;
```

检查关键表是否存在：

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('forum_interest_partition', 'forum_chat_room');
```

检查分区基础数据：

```sql
SELECT id, partition_code, partition_name, sort_order, status
FROM forum_interest_partition
ORDER BY sort_order, id;
```

检查聊天室与分区关联是否完整：

```sql
SELECT room_code, room_name, partition_id
FROM forum_chat_room
ORDER BY room_code;
```

检查 `forum_chat_room` 的分区索引与外键：

```sql
SELECT index_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'forum_chat_room'
  AND index_name = 'idx_forum_chat_room_partition_id';

SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name = 'forum_chat_room'
  AND constraint_name = 'fk_forum_chat_room_partition_id';
```

若 `forum_interest_partition` 缺失，执行修复：

```sql
SOURCE sql/v1.2_interest_partition_migration.sql;
```
