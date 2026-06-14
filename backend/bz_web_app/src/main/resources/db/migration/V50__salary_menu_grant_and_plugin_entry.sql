-- ============================================
-- BareZen OA V50: 工资/插件菜单授权与数据修正
-- ============================================
-- 背景（界面缺口盘查结论）：
--   1) V47 插入的工资管理菜单 10031-10042 未授权任何角色（含 admin），
--      导致工资模块配置了菜单也无人可见；
--   2) sys_menu 存在历史手工插入的幽灵记录 10022（"工资统计"错挂在
--      资产分类 10019 下），与 V47 的 10034（挂在工资管理 10031 下）重复，
--      予以逻辑删除；
--   3) 插件管理端点（/api/system/v1/plugins，权限 system:plugin:manage）
--      自始无菜单入口，补 C 菜单并授权 admin。
--
-- 幂等性：菜单用 INSERT IGNORE（固定 id）；授权用 INSERT IGNORE ... SELECT
-- （仅对已存在的菜单行生成授权，不同环境库均可安全重放）。
-- 状态语义：status '0'=正常 '1'=停用（RuoYi 约定，V48 已归一化存量）。

-- 1. 幽灵菜单清理：10022 为手工插入（不在任何迁移中），错挂资产分类下
UPDATE `sys_menu`
SET `del_flag` = 1,
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [V50] 与 10034 重复，错挂资产分类，逻辑删除')
WHERE `id` = 10022 AND `del_flag` = 0 AND `parent_id` = 10019;

-- 2. 插件管理菜单（挂在系统管理 id=1 下；10273 起为空闲段，现最大 10272）
INSERT IGNORE INTO `sys_menu`
(`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10273, 1, '插件管理', 'C', 'system:plugin:manage', 'system/plugin', 'system/plugin/index', 'plugin', 9, '0', '0', '插件启停管理（V50 补入口）'),
(10274, 10273, '插件管理操作', 'F', 'system:plugin:manage', '', '', '#', 1, '0', '0', '');

-- 3. admin 角色（role_id=1）授权：工资全量菜单（V47 遗留缺口）+ 本迁移新增菜单。
--    仅对库中实际存在的菜单行生成授权，兼容不同环境的差异
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m
WHERE m.`del_flag` = 0
  AND (m.`id` BETWEEN 10031 AND 10042 OR m.`id` IN (10273, 10274));
