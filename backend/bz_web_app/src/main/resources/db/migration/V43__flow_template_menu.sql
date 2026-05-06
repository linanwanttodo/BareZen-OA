-- ============================================
-- BareZen_Base V43: 审批流程模板菜单 + 按钮权限（2.1e 管理页补全）
-- ============================================
-- 背景：2.1e 已提供审批模板 CRUD API（ApprovalFlowTemplateController，
--       /api/system/v1/flow-templates，权限 system:flow:template:*），
--       但 V36 仅种子数据、未配菜单，管理员只能靠 SQL 配置多级审批链。
--       本迁移补齐：
--         1) 「审批流程模板」C 菜单（挂系统管理 id=1 下，order 8，id=10021）
--         2) F 类型按钮权限：system:flow:template:{list,add,edit,delete}
-- 说明：菜单 id 10021 为当前空闲段（10017 开放平台 / 10018 考勤统计 /
--       10019 资产分类 / 10020 知识分类 之后）；F 按钮自 10269 起（10252-10268 已用）。

-- 1. 「审批流程模板」C 菜单（挂系统管理 id=1 下，order 8）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10021, 1, '审批流程模板', 'C', 'system:flow:template:list', 'flow-template', 'system/flow-template/index', 'safe', 8, '0', '1', '审批流程模板管理菜单（配置各类业务单的审批节点链，多行=多级）');

-- 2. F 类型按钮权限（挂审批流程模板 10021 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10269, 10021, '模板列表', 'F', 'system:flow:template:list', NULL, NULL, '#', 1, '0', '1', '审批模板节点列表/查看'),
(10270, 10021, '模板新增', 'F', 'system:flow:template:add', NULL, NULL, '#', 2, '0', '1', '审批模板节点新增'),
(10271, 10021, '模板修改', 'F', 'system:flow:template:edit', NULL, NULL, '#', 3, '0', '1', '审批模板节点修改'),
(10272, 10021, '模板删除', 'F', 'system:flow:template:delete', NULL, NULL, '#', 4, '0', '1', '审批模板节点删除');

-- 3. admin 角色授权（审批流程模板菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 10021);