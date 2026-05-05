-- ============================================
-- BareZen_Base V33: 开放平台管理菜单放开显示
-- ============================================
-- 背景：V32 开放平台菜单（id=10017）以 visible='1' 隐藏入库，等待前端
--       system/openapi/index 页面就绪；现页面已完成，翻转 visible='0' 显示。
UPDATE `sys_menu` SET `visible` = '0',
       `remark` = '开放平台管理菜单（API Key/Webhook 管理）'
 WHERE `id` = 10017;