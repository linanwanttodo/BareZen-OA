-- ============================================
-- BareZen_Base V15: 统一待办中心
-- ============================================
-- 公司内部管理系统：聚合各业务模块待审批事项（请假/用车等）。

-- 1. 待办中心菜单（顶级菜单，紧跟仪表盘与系统管理之后）
-- 注意：菜单 id 必须 >= 10000（V6 已占用 1-21，V14 已占用 10001-10228）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10009, 0, '待办中心', 'C', 'system:todo:list', 'todo-center', 'todo-center/index', 'notification', 2, '0', '1', '统一待办中心菜单');

-- 2. admin 角色授予待办中心权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 10009);
