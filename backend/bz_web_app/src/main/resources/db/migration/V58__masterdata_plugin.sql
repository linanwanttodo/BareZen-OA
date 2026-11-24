-- ============================================
-- BareZen_Base V58: 主数据底座（供应商/客户/物料）+ 单据号规则
-- ============================================
-- 插件ID: com.linan.plugin.masterdata（第一个走插件门控机制的业务插件：
--   清单 resources/plugins/masterdata-plugin.json 声明 controller/权限/路由，
--   PluginGateAspect 按包名 com.linan.plugin.masterdata 门控其全部接口）
--
-- 表风格对齐 V47 salary 插件契约（V56 起物理删除）：
--   del_flag 仅作历史列保留、不标 @TableLogic，removeById 即物理删除，
--   编码唯一键不含 del_flag，删除后可重建同编码记录。
--
-- 幂等性：建表 IF NOT EXISTS；菜单/规则 INSERT IGNORE；授权只对库中存在且
--         未删除的行生效，可安全重放。
-- 菜单 ID：顺延现有 sys_menu 最大 ID 10282（V57），本迁移占用 10283-10298。
-- path 约定：子菜单 path 含父前缀（masterdata/supplier），见 V51/V53 注释。
-- 图标取值：必须命中前端 utils/menuIcon.ts 的 MENU_ICONS 映射表，
--           未登记的取值会退回 FolderIcon。
-- 状态语义：status '0'=正常 '1'=停用（V48 已归一化）。

-- 1. 供应商信息表
CREATE TABLE IF NOT EXISTS supplier_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '供应商ID',
    supplier_code VARCHAR(32) NOT NULL COMMENT '供应商编码',
    supplier_name VARCHAR(100) NOT NULL COMMENT '供应商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(1) DEFAULT '0' COMMENT '状态: 0=正常, 1=停用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记（物理删除契约，仅历史列）',
    UNIQUE KEY uk_supplier_code (supplier_code),
    KEY idx_supplier_name (supplier_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商信息表';

-- 2. 客户信息表
CREATE TABLE IF NOT EXISTS customer_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_code VARCHAR(32) NOT NULL COMMENT '客户编码',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(1) DEFAULT '0' COMMENT '状态: 0=正常, 1=停用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记（物理删除契约，仅历史列）',
    UNIQUE KEY uk_customer_code (customer_code),
    KEY idx_customer_name (customer_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';

-- 3. 物料信息表
CREATE TABLE IF NOT EXISTS material_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '物料ID',
    material_code VARCHAR(32) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    status VARCHAR(1) DEFAULT '0' COMMENT '状态: 0=正常, 1=停用',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记（物理删除契约，仅历史列）',
    UNIQUE KEY uk_material_code (material_code),
    KEY idx_material_name (material_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料信息表';

-- 4. 单据号规则表（DocNumberService 行锁发号，供采购/销售等后续模块复用）
--    last_date 记录最近发号日期串：跨日发号时序号从 0 重置（题目五字段之外
--    的必要补充，否则无法实现 SUP20261124-0001 的按日重置语义）。
CREATE TABLE IF NOT EXISTS sys_doc_number_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    biz_key VARCHAR(32) NOT NULL COMMENT '业务键（供应商SUP/客户CUS/物料MAT/采购单PO等）',
    prefix VARCHAR(16) NOT NULL COMMENT '单据号前缀',
    date_format VARCHAR(32) NOT NULL DEFAULT 'yyyyMMdd' COMMENT '日期段格式（java.time.format.DateTimeFormatter）',
    current_seq BIGINT NOT NULL DEFAULT 0 COMMENT '当前序号（该日期段内已发最大序号）',
    step INT NOT NULL DEFAULT 1 COMMENT '步长',
    last_date VARCHAR(32) COMMENT '最近发号日期串（跨日重置序号的依据）',
    remark VARCHAR(200) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记（物理删除契约，仅历史列）',
    UNIQUE KEY uk_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单据号规则表';

-- 5. 种子单据号规则：供应商/客户/物料（主数据编码可走 DocNumberService 发号）
INSERT IGNORE INTO sys_doc_number_rule
  (`id`, `biz_key`, `prefix`, `date_format`, `current_seq`, `step`, `last_date`, `remark`)
VALUES
  (1, 'SUP', 'SUP', 'yyyyMMdd', 0, 1, NULL, '供应商单据号规则'),
  (2, 'CUS', 'CUS', 'yyyyMMdd', 0, 1, NULL, '客户单据号规则'),
  (3, 'MAT', 'MAT', 'yyyyMMdd', 0, 1, NULL, '物料单据号规则');

-- 6. 菜单：主数据目录 + 供应商/客户/物料三个页面（ID 顺延 10282，从 10283 起）
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`)
VALUES
  (10283, 0, '主数据', 'M', NULL, 'masterdata', NULL, 'apps', 11, '0', '0', '主数据底座目录（供应商/客户/物料）'),
  (10284, 10283, '供应商', 'C', 'masterdata:supplier:list', 'masterdata/supplier', 'masterdata/supplier/index', 'peoples', 1, '0', '0', '供应商信息管理'),
  (10285, 10283, '客户', 'C', 'masterdata:customer:list', 'masterdata/customer', 'masterdata/customer/index', 'user', 2, '0', '0', '客户信息管理'),
  (10286, 10283, '物料', 'C', 'masterdata:material:list', 'masterdata/material', 'masterdata/material/index', 'tags', 3, '0', '0', '物料信息管理');

-- 7. 按钮权限：三类主数据各 list/add/edit/delete（ID 10287-10298）
INSERT IGNORE INTO `sys_menu`
  (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`)
VALUES
  (10287, 10284, '供应商查询', 'F', 'masterdata:supplier:list', '', '', '#', 1, '0', '0', ''),
  (10288, 10284, '供应商新增', 'F', 'masterdata:supplier:add', '', '', '#', 2, '0', '0', ''),
  (10289, 10284, '供应商修改', 'F', 'masterdata:supplier:edit', '', '', '#', 3, '0', '0', ''),
  (10290, 10284, '供应商删除', 'F', 'masterdata:supplier:delete', '', '', '#', 4, '0', '0', ''),
  (10291, 10285, '客户查询', 'F', 'masterdata:customer:list', '', '', '#', 1, '0', '0', ''),
  (10292, 10285, '客户新增', 'F', 'masterdata:customer:add', '', '', '#', 2, '0', '0', ''),
  (10293, 10285, '客户修改', 'F', 'masterdata:customer:edit', '', '', '#', 3, '0', '0', ''),
  (10294, 10285, '客户删除', 'F', 'masterdata:customer:delete', '', '', '#', 4, '0', '0', ''),
  (10295, 10286, '物料查询', 'F', 'masterdata:material:list', '', '', '#', 1, '0', '0', ''),
  (10296, 10286, '物料新增', 'F', 'masterdata:material:add', '', '', '#', 2, '0', '0', ''),
  (10297, 10286, '物料修改', 'F', 'masterdata:material:edit', '', '', '#', 3, '0', '0', ''),
  (10298, 10286, '物料删除', 'F', 'masterdata:material:delete', '', '', '#', 4, '0', '0', '');

-- 8. 授权给超级管理员（role_id=1），沿用 V53/V57 写法
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m
WHERE m.`del_flag` = 0
  AND m.`id` BETWEEN 10283 AND 10298;
