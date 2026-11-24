-- ============================================
-- BareZen_Base V55: sys_file 增加归属查询索引
-- ============================================
-- 背景：用户前台「我的文件」列表按 create_by（上传人）过滤并按 create_time 倒序，
--       sys_file 现有索引仅有 idx_folder_id / idx_scope，
--       数据量上来后按归属过滤会退化为全表扫描。
--       复用既有 sys_file 表，不新增列：归属沿用 BaseEntity.create_by。
--
-- 幂等性：MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，
--       用 information_schema 判断索引是否已存在，存在则跳过，可安全重放。

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_file'
      AND INDEX_NAME = 'idx_create_by'
);

SET @ddl := IF(@idx_exists > 0,
    'SELECT 1',
    'ALTER TABLE `sys_file` ADD INDEX `idx_create_by` (`create_by`)');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
