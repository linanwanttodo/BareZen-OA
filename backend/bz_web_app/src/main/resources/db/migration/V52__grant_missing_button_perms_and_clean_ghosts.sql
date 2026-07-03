-- ============================================
-- BareZen_Base V52: 补齐按钮级授权 + 清理工资模块重复幽灵菜单
-- ============================================
-- 背景（前端按钮不可见的根因排查）：
--   1) V40/V42/V43 只把 C 型菜单授给 admin（role_id=1），漏授其下的 F 型按钮，
--      导致 v-permission 与后端 @RequirePermission 双双拦住：资产分类、知识分类、
--      审批流程模板、日程、资产借用共 13 个按钮对管理员完全不显示；
--   2) 10023-10030 是工资模块按钮的早期错插行（挂在知识分类 10020、审批流程模板
--      10021、工资统计 10022 之下），与 V47 的 10035-10042 权限串完全重复且已正确
--      授权，属幽灵记录。V50 已按同样理由逻辑删除了 10022，本迁移补齐其余 8 行。
--
-- 幂等性：授权用 INSERT IGNORE；逻辑删除带 del_flag=0 与 parent_id 限定，可安全重放。
-- 状态语义：status '0'=正常 '1'=停用（RuoYi 约定，V48 已归一化存量）。

-- 1. 补授 admin 缺失的按钮级权限（仅对库中确实存在且未删除的行生效）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`id` FROM `sys_menu` m
WHERE m.`del_flag` = 0
  AND m.`menu_type` = 'F'
  AND m.`id` IN (10260, 10261, 10262, 10263, 10264, 10265, 10266,
                 10267, 10268, 10269, 10270, 10271, 10272);

-- 2. 逻辑删除工资模块重复幽灵按钮（10035-10042 已覆盖同样权限串并授权）
UPDATE `sys_menu`
SET `del_flag` = 1,
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [V52] 与 10035-10042 重复且错挂父级，逻辑删除')
WHERE `del_flag` = 0
  AND `menu_type` = 'F'
  AND `id` BETWEEN 10023 AND 10030
  AND `perms` LIKE 'salary:%';
