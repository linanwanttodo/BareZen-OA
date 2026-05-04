-- ============================================================
-- V28: 修复菜单权限串与后端 @RequirePermission 不一致
-- ------------------------------------------------------------
-- 1) 代码生成菜单 id=12：perms 由 'tool:gen:list' 改为 'system:gen:list'
--    （后端 GenController 使用 @RequirePermission("system:gen:list")，
--      原菜单权限串无人授予导致非 admin 角色访问 403）
-- 2) 操作日志菜单 id=20：perms 由 'monitor:oper-log:list' 改为 'monitor:operlog:list'
--    （后端 SysOperLogController 与前端 v-permission 均使用无连字符的
--      "monitor:operlog:list"，原菜单带连字符导致权限不匹配）
-- ============================================================

UPDATE `sys_menu` SET `perms` = 'system:gen:list' WHERE `id` = 12 AND `perms` = 'tool:gen:list';
UPDATE `sys_menu` SET `perms` = 'monitor:operlog:list' WHERE `id` = 20 AND `perms` = 'monitor:oper-log:list';
