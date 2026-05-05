-- ============================================
-- BareZen_Base V35: 考勤统计菜单（管理端入口）
-- ============================================
-- 背景：T1.3 已实现管理端考勤统计 API（GET /api/hr/v1/clock/page，
--       权限串 hr:attendance:list），本迁移补管理端菜单入口：
--       1) 「考勤统计」C 菜单（挂人事管理 id=10001 下，order 7，id=10018）
--       2) admin 角色授权
-- 说明：页面无新增/修改/删除等写操作，无需 F 类型按钮权限；
--       C 菜单 perms 与后端 @RequirePermission("hr:attendance:list") 完全一致。

-- 1. 「考勤统计」C 菜单（挂人事管理 id=10001 下，order 7）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10018, 10001, '考勤统计', 'C', 'hr:attendance:list', 'hr/attendance', 'hr/attendance/index', 'clock', 7, '0', '1', '月度考勤统计菜单（按员工分组聚合出勤/迟到/早退/缺卡）');

-- 2. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10018);