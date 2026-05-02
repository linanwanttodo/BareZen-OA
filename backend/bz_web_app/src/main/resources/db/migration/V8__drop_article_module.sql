-- ============================================
-- BareZen_Base V8: 移除文章模块
-- ============================================
-- 公司内部管理系统不再需要文章功能，删除文章相关数据表与菜单。
-- 历史迁移（V2/V3）保持不可变，这里通过增量迁移清理，兼容已部署环境。

-- 1. 删除文章相关数据表（先删子表，再删主表）
DROP TABLE IF EXISTS `art_read_history`;
DROP TABLE IF EXISTS `art_favorite`;
DROP TABLE IF EXISTS `art_comment`;
DROP TABLE IF EXISTS `art_article`;
DROP TABLE IF EXISTS `art_category`;

-- 2. 清理文章相关菜单（id=5 内容管理目录, id=6 文章管理菜单，见 V3 种子数据）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (5, 6);
DELETE FROM `sys_menu` WHERE `id` IN (5, 6);
