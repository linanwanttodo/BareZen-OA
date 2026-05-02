-- ============================================
-- BareZen_Base V6: 补充菜单数据 & 修复菜单路径
-- ============================================
-- 说明：使 sys_menu 数据与前端静态路由保持一致，
-- 支持纯动态路由加载模式。

-- 1. 修复现有菜单的 path，使其匹配完整的路由路径
UPDATE `sys_menu` SET `path` = 'system/user'    WHERE `id` = 2;
UPDATE `sys_menu` SET `path` = 'system/role'    WHERE `id` = 3;
UPDATE `sys_menu` SET `path` = 'system/menu'    WHERE `id` = 4;
UPDATE `sys_menu` SET `path` = 'system/config'  WHERE `id` = 7;
UPDATE `sys_menu` SET `path` = 'content/article' WHERE `id` = 6;
UPDATE `sys_menu` SET `path` = 'monitor/logininfor' WHERE `id` = 9;
UPDATE `sys_menu` SET `path` = 'monitor/jobs'   WHERE `id` = 10;
UPDATE `sys_menu` SET `path` = 'monitor/backup'  WHERE `id` = 13;
UPDATE `sys_menu` SET `path` = 'tool/gen'        WHERE `id` = 12;

-- 2. 新增菜单：仪表盘
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(14, 0, '仪表盘', 'C', NULL, 'dashboard', 'dashboard/index', 'dashboard', 0, '0', '1', '仪表盘首页');

-- 3. 新增菜单：字典管理（归属 系统管理 目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(15, 1, '字典管理', 'C', 'system:dict:list', 'system/dict', 'system/dict/index', 'list', 5, '0', '1', '字典管理菜单');

-- 4. 新增菜单：部门管理（归属 系统管理 目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(16, 1, '部门管理', 'C', 'system:dept:list', 'system/dept', 'system/dept/index', 'tree', 6, '0', '1', '部门管理菜单');

-- 5. 新增菜单：权限分配（归属 系统管理 目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(17, 1, '权限分配', 'C', 'system:role:dept', 'system/role-dept', 'system/role-dept/index', 'peoples', 7, '0', '1', '角色-部门-用户分配菜单');

-- 6. 新增菜单：通知公告（顶级菜单）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(18, 0, '通知公告', 'C', 'system:notice:list', 'notice', 'notice/index', 'notification', 5, '0', '1', '通知公告菜单');

-- 7. 新增菜单：在线用户（归属 监控管理 目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(19, 8, '在线用户', 'C', 'monitor:online:list', 'monitor/online', 'monitor/online/index', 'desktop', 4, '0', '1', '在线用户菜单');

-- 8. 新增菜单：操作日志（归属 监控管理 目录）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(20, 8, '操作日志', 'C', 'monitor:oper-log:list', 'monitor/oper-log', 'monitor/oper-log/index', 'file-text', 5, '0', '1', '操作日志菜单');

-- 9. 新增菜单：个人中心（顶级菜单，隐藏）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(21, 0, '个人中心', 'C', NULL, 'profile', 'profile/index', 'user', 99, '1', '1', '个人中心菜单(隐藏)');

-- 10. 为 admin 角色关联所有新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21);

-- 自增起始值
ALTER TABLE `sys_menu` AUTO_INCREMENT = 10000;
