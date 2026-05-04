-- ============================================
-- BareZen_Base V29: 补齐「文件管理」菜单入口
-- ============================================
-- 背景：SysFileController（/api/system/v1/files）已完整实现上传/下载/文件夹/
-- 重命名/删除/树/按文件夹分页等接口，sys_file 表与根目录种子数据也已就绪，
-- 但缺少前端页面与菜单入口，导致后端能力悬空（导航栏找不到入口）。
-- 方案：新增「文件管理」C 菜单（挂系统管理 id=1 下，order 5），
-- 并补齐 5 个 F 按钮权限（上传/新建文件夹/修改/删除/下载），最后给 admin 角色授权。

-- 1. 新增「文件管理」菜单（component=file/index -> frontend_admin/src/views/file/index.vue）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10016, 1, '文件管理', 'C', 'system:file:list', 'file', 'file/index', 'folder', 5, '0', '1', '文件管理菜单');

-- 2. 文件管理按钮权限（F 类型，供角色授权按需分配）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10247, 10016, '文件上传', 'F', 'system:file:upload', NULL, NULL, '#', 1, '0', '1', '文件上传按钮'),
(10248, 10016, '新建文件夹', 'F', 'system:file:add', NULL, NULL, '#', 2, '0', '1', '新建文件夹按钮'),
(10249, 10016, '文件修改', 'F', 'system:file:edit', NULL, NULL, '#', 3, '0', '1', '重命名按钮'),
(10250, 10016, '文件删除', 'F', 'system:file:delete', NULL, NULL, '#', 4, '0', '1', '删除按钮'),
(10251, 10016, '文件下载', 'F', 'system:file:download', NULL, NULL, '#', 5, '0', '1', '下载按钮');

-- 3. admin 角色授权（role_id=1 超级管理员）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10016),
(1, 10247),
(1, 10248),
(1, 10249),
(1, 10250),
(1, 10251);
