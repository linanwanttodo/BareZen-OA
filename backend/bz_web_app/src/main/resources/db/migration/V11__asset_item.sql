-- ============================================
-- BareZen_Base V11: 资产管理模块
-- ============================================
-- 公司内部管理系统：固定资产台账，覆盖在库/使用/维修/报废全生命周期。

-- 1. 资产台账表
CREATE TABLE IF NOT EXISTS `asset_item` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `asset_no` VARCHAR(64) NOT NULL COMMENT '资产编号',
  `asset_name` VARCHAR(100) NOT NULL COMMENT '资产名称',
  `category` VARCHAR(20) DEFAULT 'other' COMMENT '资产分类：computer=电脑办公 equipment=设备 furniture=家具 vehicle=车辆 other=其他',
  `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
  `model` VARCHAR(50) DEFAULT NULL COMMENT '型号',
  `price` DECIMAL(12,2) DEFAULT 0 COMMENT '购置价格（元）',
  `purchase_date` VARCHAR(20) DEFAULT NULL COMMENT '购置日期',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '使用部门',
  `holder_name` VARCHAR(50) DEFAULT NULL COMMENT '使用人',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '存放位置',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=在库 1=使用中 2=维修中 3=已报废',
  `scrap_date` VARCHAR(20) DEFAULT NULL COMMENT '报废日期',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_no` (`asset_no`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产台账表';

-- 2. 资产管理菜单（id=17 目录, id=18 资产台账菜单）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(17, 0, '资产管理', 'M', NULL, 'asset', NULL, 'appstore', 6, '0', '1', '资产管理目录'),
(18, 17, '资产台账', 'C', 'asset:item:list', 'item', 'asset/item/index', 'storage', 1, '0', '1', '资产台账菜单');

-- 3. admin 角色授予资产管理权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 17), (1, 18);

-- 4. 示例资产数据
INSERT IGNORE INTO `asset_item` (`id`, `asset_no`, `asset_name`, `category`, `brand`, `model`, `price`, `purchase_date`, `dept_name`, `holder_name`, `location`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, 'AST-2025-0001', '办公台式电脑', 'computer', '联想', 'ThinkCentre M760', 4999.00, '2025-03-15', '总公司', '超级管理员', '3F-301', '1', NOW(), NOW(), 1, 1, 0);
