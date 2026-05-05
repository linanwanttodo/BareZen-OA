-- ============================================
-- BareZen_Base V42: 知识库分类菜单 + 按钮权限（2.3）
-- ============================================
-- 背景：2.3 新增知识库分类管理页（frontend_admin kb/category）与文档版本历史
--       能力（KbDocumentController 新增 /{id}/versions，更新前写版本快照）。
--       本迁移补齐：
--         1) 「知识分类」C 菜单（挂知识库 22 下，order 2，id=10020）
--         2) F 类型按钮权限：知识分类 CRUD（kb:category:*）
-- 说明：原计划 V39/V40 已被资产占用，本迁移顺延 V42（V41 为知识库分类+版本表）。

-- 1. 「知识分类」C 菜单（挂知识库 id=22 下，order 2）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10020, 22, '知识分类', 'C', 'kb:category:list', 'category', 'kb/category/index', 'tags', 2, '0', '1', '知识库分类管理菜单（分类树）');

-- 2. F 类型按钮权限（挂知识分类 10020 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10266, 10020, '分类新增', 'F', 'kb:category:add', NULL, NULL, '#', 1, '0', '1', '知识库分类新增按钮'),
(10267, 10020, '分类修改', 'F', 'kb:category:edit', NULL, NULL, '#', 2, '0', '1', '知识库分类修改按钮'),
(10268, 10020, '分类删除', 'F', 'kb:category:delete', NULL, NULL, '#', 3, '0', '1', '知识库分类删除按钮');

-- 3. admin 角色授权（知识分类菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 10020);