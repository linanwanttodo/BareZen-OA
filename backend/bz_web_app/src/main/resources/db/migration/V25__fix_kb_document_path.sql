-- ============================================
-- BareZen_Base V25: 修正知识文档菜单路由路径
-- ============================================
-- 背景：V13 插入知识库（id 22, path='kb'）与知识文档（id 23, path='document'），
-- 知识文档缺少父前缀 'kb/'，与其余菜单「path 含完整父前缀」的约定不一致
-- （如 system/user、hr/employee）。前端跳转 /kb/document 时无对应路由，
-- 落入 catch-all 404。
-- 历史迁移（V13）不可变，这里通过增量 UPDATE 修正，仅影响 id=23
-- 且路径未被人工修改过的行，不触碰其他菜单。

UPDATE `sys_menu`
SET `path` = 'kb/document'
WHERE `id` = 23
  AND `parent_id` = 22
  AND `path` = 'document'
  AND `component` = 'kb/document/index';
