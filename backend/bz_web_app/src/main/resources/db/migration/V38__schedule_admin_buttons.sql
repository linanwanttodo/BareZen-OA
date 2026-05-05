-- ============================================
-- BareZen_Base V38: 日程管理新增/编辑按钮权限（2.4）
-- ============================================
-- 背景：2.4 为管理端日程页（frontend_admin hr/schedule）增加「新增/编辑」能力，
--       后端 HrScheduleController 新增 POST /api/hr/v1/schedules 与
--       PUT /api/hr/v1/schedules/{id}（权限 hr:schedule:add / hr:schedule:edit）。
--       本迁移补齐两个 F 类型按钮权限（挂日程管理 10014 下，沿用 V26 写法），
--       与前端 v-permission 一致。原计划 V37 已被 approval_flow_log 补列占用，
--       本迁移顺延为 V38。

-- 1. F 类型按钮权限（挂「日程管理」10014 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10260, 10014, '日程新增', 'F', 'hr:schedule:add', NULL, NULL, '#', 2, '0', '1', '日程新增按钮（管理端代建）'),
(10261, 10014, '日程修改', 'F', 'hr:schedule:edit', NULL, NULL, '#', 3, '0', '1', '日程修改按钮');