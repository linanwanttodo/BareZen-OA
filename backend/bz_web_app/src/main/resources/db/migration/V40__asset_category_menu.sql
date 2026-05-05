-- ============================================
-- BareZen_Base V40: 资产分类菜单 + 借用按钮权限（2.2）
-- ============================================
-- 背景：2.2 新增资产分类管理页（frontend_admin asset/category）与资产借用/归还
--       能力（AssetItemController 新增 /borrow、/return、/borrow-records）。
--       本迁移补齐：
--         1) 「资产分类」C 菜单（挂资产管理 17 下，order 2，id=10019）
--         2) F 类型按钮权限：资产借用（asset:item:borrow）、分类 CRUD（asset:category:*）
-- 说明：原计划 V38 已被日程按钮权限占用、V39 已被资产分类+借用表占用，本迁移顺延 V40。

-- 1. 「资产分类」C 菜单（挂资产管理 id=17 下，order 2）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10019, 17, '资产分类', 'C', 'asset:category:list', 'category', 'asset/category/index', 'tags', 2, '0', '1', '资产分类管理菜单（分类树）');

-- 2. F 类型按钮权限
--    资产台账（18）下：借用/归还
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10262, 18, '资产借用', 'F', 'asset:item:borrow', NULL, NULL, '#', 5, '0', '1', '资产借用/归还按钮');
--    资产分类（10019）下：新增/修改/删除
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10263, 10019, '分类新增', 'F', 'asset:category:add', NULL, NULL, '#', 1, '0', '1', '资产分类新增按钮'),
(10264, 10019, '分类修改', 'F', 'asset:category:edit', NULL, NULL, '#', 2, '0', '1', '资产分类修改按钮'),
(10265, 10019, '分类删除', 'F', 'asset:category:delete', NULL, NULL, '#', 3, '0', '1', '资产分类删除按钮');

-- 3. admin 角色授权（资产分类菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 10019);