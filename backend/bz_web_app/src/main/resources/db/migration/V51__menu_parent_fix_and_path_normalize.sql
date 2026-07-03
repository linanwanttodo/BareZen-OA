-- ============================================
-- BareZen_Base V51: 菜单挂载纠错 + 子菜单 path 约定归一
-- ============================================
-- 背景（前端路由一致性盘查结论）：
--   1) V40 的注释写着「资产分类挂资产管理 17 下」，但 V3 种子里 id=17 是
--      「权限分配」，资产管理实际是 10004。结果 10019 资产分类错挂在
--      系统管理 > 权限分配 之下，且其 F 按钮 10262「资产借用」同样按
--      错误编号挂到了 18「通知公告」下；
--   2) 早期迁移插入的子菜单 path 用了「相对父级」约定（category / file /
--      structure 等），与主流的「已含父前缀」约定（system/user）混用。
--      前端 resolveMenuPath 会拼出 /kb/category，而相对 path 在 vue-router
--      中是绝对路径，实际注册的是 /category —— 点击菜单直接 404；不同父级下
--      的同名相对 path（两处 category、两处 statistics）还会互相遮蔽。
--
-- 处理方式：前端已统一由 resolveMenuPath 解析路由 path（兼容两种约定），
--   本迁移同时把数据归一到「子菜单 path 含父前缀」这一主流约定，消除歧义源。
-- 幂等性：全部为带当前值限定的 UPDATE，可安全重放；不改任何 id 与权限字符串。
-- 状态语义：status '0'=正常 '1'=停用（RuoYi 约定，V48 已归一化存量）。

-- 1. 资产分类（10019）改挂资产管理（10004），path 补齐父前缀
UPDATE `sys_menu`
SET `parent_id` = 10004,
    `path` = 'asset/category',
    `order_num` = 2,
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [V51] 原错挂权限分配(17)，改挂资产管理(10004)')
WHERE `id` = 10019 AND `parent_id` = 17;

-- 2. 资产借用按钮（10262）改挂资产台账（10005），原先错挂在通知公告(18)
UPDATE `sys_menu`
SET `parent_id` = 10005,
    `remark` = CONCAT(IFNULL(`remark`, ''), ' [V51] 原错挂通知公告(18)，改挂资产台账(10005)')
WHERE `id` = 10262 AND `parent_id` = 18;

-- 3. 子菜单 path 归一为「含父前缀」约定（相对约定会让 vue-router 注册成顶层绝对路径）
UPDATE `sys_menu` SET `path` = 'system/file'          WHERE `id` = 10016 AND `path` = 'file'         AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'system/openapi'       WHERE `id` = 10017 AND `path` = 'openapi'      AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'system/flow-template' WHERE `id` = 10021 AND `path` = 'flow-template' AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'kb/category'          WHERE `id` = 10020 AND `path` = 'category'     AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'salary/structure'     WHERE `id` = 10032 AND `path` = 'structure'    AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'salary/payment'       WHERE `id` = 10033 AND `path` = 'payment'      AND `del_flag` = 0;
UPDATE `sys_menu` SET `path` = 'salary/statistics'    WHERE `id` = 10034 AND `path` = 'statistics'   AND `del_flag` = 0;
