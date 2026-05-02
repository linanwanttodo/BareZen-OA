-- ============================================
-- BareZen_Base V19: 办公用品申领
-- ============================================
-- 公司内部管理系统：办公用品入库→员工申领→审批→领用记录。

-- 1. 办公用品表
CREATE TABLE IF NOT EXISTS `office_supply` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `supply_name` VARCHAR(100) NOT NULL COMMENT '用品名称',
  `category` VARCHAR(50) DEFAULT 'other' COMMENT '分类：stationery=文具 electronics=电子耗材 cleaning=清洁用品 other=其他',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `unit` VARCHAR(20) DEFAULT '个' COMMENT '单位',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=正常 1=停用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_supply_name` (`supply_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='办公用品表';

-- 2. 申领记录表
CREATE TABLE IF NOT EXISTS `office_supply_request` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `supply_id` BIGINT NOT NULL COMMENT '用品ID',
  `supply_name` VARCHAR(100) DEFAULT NULL COMMENT '用品名称（冗余）',
  `user_id` BIGINT NOT NULL COMMENT '申领人用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '申领人姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申领人部门（冗余）',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '申领数量',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '申领事由',
  `status` VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '状态：0=待审批 1=已通过 2=已驳回 3=已取消',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人用户ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approve_time` VARCHAR(20) DEFAULT NULL COMMENT '审批时间',
  `approve_remark` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_supply_id` (`supply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申领记录表';

-- 3. 办公用品菜单（id=10013，挂载在行政办公目录 id=10006 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10013, 10006, '办公用品', 'C', 'office:supply:list', 'office/supply', 'office/supply/index', 'apps', 3, '0', '1', '办公用品菜单');

-- 4. F 类型按钮权限
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10241, 10013, '用品入库', 'F', 'office:supply:add', NULL, NULL, '#', 1, '0', '1', '用品入库按钮'),
(10242, 10013, '用品修改', 'F', 'office:supply:edit', NULL, NULL, '#', 2, '0', '1', '用品修改按钮'),
(10243, 10013, '用品删除', 'F', 'office:supply:delete', NULL, NULL, '#', 3, '0', '1', '用品删除按钮'),
(10244, 10013, '申领审批', 'F', 'office:supply:approve', NULL, NULL, '#', 4, '0', '1', '申领审批按钮');

-- 5. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10013),
(1, 10241), (1, 10242), (1, 10243), (1, 10244);

-- 6. 示例用品
INSERT IGNORE INTO `office_supply` (`id`, `supply_name`, `category`, `spec`, `stock`, `unit`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`) VALUES
(1, 'A4复印纸', 'stationery', '80g/包', 50, '包', '0', NOW(), NOW(), 1, 1, 0),
(2, '黑色签字笔', 'stationery', '0.5mm', 100, '支', '0', NOW(), NOW(), 1, 1, 0),
(3, '打印硒鼓', 'electronics', 'HP-88A', 10, '个', '0', NOW(), NOW(), 1, 1, 0);
