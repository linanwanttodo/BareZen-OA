-- ============================================
-- BareZen_Base V32: 开放平台管理菜单（API Key / Webhook 管理入口）
-- ============================================
-- 背景：V30 建了 sys_api_key / sys_webhook 表并实现开放接口校验侧（OpenApiInterceptor），
--       但全库无任何管理接口，API Key 只能手工插库。本迁移补管理端菜单与按钮权限：
--       1) 「开放平台」C 菜单（挂系统管理 id=1 下，id=10017；前端页面未就绪前
--          visible='1' 隐藏，避免侧边栏出现死链，前端完成后翻转 visible='0'）
--       2) API Key / Webhook 的 F 类型按钮权限（id 从 10252 起，避开 V29 已用 10247-10251）
-- 权限串与后端 @RequirePermission 完全一致：
--   system:apiKey:{list,add,edit,delete} / system:webhook:{list,add,edit,delete}

-- 1. 「开放平台」C 菜单（挂系统管理 id=1 下，order 7）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10017, 1, '开放平台', 'C', 'system:openapi:list', 'openapi', 'system/openapi/index', 'link', 7, '1', '1', '开放平台管理菜单（API Key/Webhook，前端页面待实现，暂隐藏）');

-- 2. F 类型按钮权限（挂「开放平台」10017 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10252, 10017, 'API密钥列表', 'F', 'system:apiKey:list', NULL, NULL, '#', 1, '0', '1', 'API Key 列表/查看'),
(10253, 10017, 'API密钥新增', 'F', 'system:apiKey:add', NULL, NULL, '#', 2, '0', '1', 'API Key 创建（明文仅返回一次）'),
(10254, 10017, 'API密钥修改', 'F', 'system:apiKey:edit', NULL, NULL, '#', 3, '0', '1', 'API Key 启停/修改'),
(10255, 10017, 'API密钥删除', 'F', 'system:apiKey:delete', NULL, NULL, '#', 4, '0', '1', 'API Key 删除'),
(10256, 10017, 'Webhook列表', 'F', 'system:webhook:list', NULL, NULL, '#', 5, '0', '1', 'Webhook 订阅列表/查看'),
(10257, 10017, 'Webhook新增', 'F', 'system:webhook:add', NULL, NULL, '#', 6, '0', '1', 'Webhook 新增订阅'),
(10258, 10017, 'Webhook修改', 'F', 'system:webhook:edit', NULL, NULL, '#', 7, '0', '1', 'Webhook 启停/修改'),
(10259, 10017, 'Webhook删除', 'F', 'system:webhook:delete', NULL, NULL, '#', 8, '0', '1', 'Webhook 删除');

-- 3. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10017),
(1, 10252), (1, 10253), (1, 10254), (1, 10255),
(1, 10256), (1, 10257), (1, 10258), (1, 10259);