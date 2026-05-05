-- ============================================
-- BareZen_Base V39: 资产分类 + 借用归还（2.2）
-- ============================================
-- 背景：V11 asset_item.category 为硬编码枚举字符串，无法扩展；无借用记录。
--       本迁移：
--         1) asset_category 分类表（种子映射现有 5 枚举，支持树形扩展）
--         2) asset_item 加 category_id 列，按枚举映射回填
--         3) asset_borrow_record 借用记录表（轻量借用，不走审批引擎）
-- 约定：asset_item.category 列保留（兼容旧查询/前端过渡），新业务按 category_id 关联。

-- 1. 资产分类表
CREATE TABLE IF NOT EXISTS `asset_category` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0=根分类，支持树形）',
  `sort` INT DEFAULT 0 COMMENT '排序号',
  `status` TINYINT DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产分类表';

-- 2. 资产台账加分类ID列
ALTER TABLE `asset_item`
  ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '资产分类ID（关联 asset_category）' AFTER `category`;

-- 3. 分类种子：映射 V11 现有 5 枚举（id 与枚举语义对应，便于回填）
INSERT IGNORE INTO `asset_category` (`id`, `name`, `parent_id`, `sort`, `status`) VALUES
(1, '电脑办公', 0, 1, 1),
(2, '设备', 0, 2, 1),
(3, '家具', 0, 3, 1),
(4, '车辆', 0, 4, 1),
(5, '其他', 0, 5, 1);

-- 4. 按枚举映射回填 category_id
UPDATE `asset_item` SET `category_id` = 1 WHERE `category` = 'computer' AND `category_id` IS NULL;
UPDATE `asset_item` SET `category_id` = 2 WHERE `category` = 'equipment' AND `category_id` IS NULL;
UPDATE `asset_item` SET `category_id` = 3 WHERE `category` = 'furniture' AND `category_id` IS NULL;
UPDATE `asset_item` SET `category_id` = 4 WHERE `category` = 'vehicle' AND `category_id` IS NULL;
UPDATE `asset_item` SET `category_id` = 5 WHERE `category` = 'other' AND `category_id` IS NULL;
UPDATE `asset_item` SET `category_id` = 5 WHERE `category_id` IS NULL;

-- 5. 资产借用记录表
CREATE TABLE IF NOT EXISTS `asset_borrow_record` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `asset_id` BIGINT NOT NULL COMMENT '资产ID',
  `borrower_id` BIGINT DEFAULT NULL COMMENT '借用人用户ID',
  `borrower_name` VARCHAR(50) DEFAULT NULL COMMENT '借用人姓名',
  `borrow_time` VARCHAR(30) DEFAULT NULL COMMENT '借用时间（yyyy-MM-dd HH:mm:ss）',
  `expect_return_time` VARCHAR(30) DEFAULT NULL COMMENT '预计归还时间',
  `return_time` VARCHAR(30) DEFAULT NULL COMMENT '实际归还时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '借用备注',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=借用中 1=已归还',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_asset` (`asset_id`),
  KEY `idx_borrower` (`borrower_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产借用记录表';