-- ============================================
-- BareZen_Base V3: 初始化数据
-- ============================================

-- 插入角色
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `create_time`, `update_time`, `del_flag`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, 4, '0', NOW(), NOW(), 0, '超级管理员拥有所有权限'),
(2, '普通用户', 'common', 2, 2, '0', NOW(), NOW(), 0, '普通用户，默认仅本部门数据权限');

-- 插入管理员用户 (密码: admin123, BCrypt加密后)
INSERT IGNORE INTO `sys_user` (`id`, `user_name`, `nick_name`, `password`, `phone`, `email`, `sex`, `avatar`, `status`, `create_time`, `update_time`, `del_flag`, `remark`) VALUES
(1, 'admin', '超级管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '13888888888', 'admin@barezen.com', '0', '', '0', NOW(), NOW(), 0, '超级管理员');

-- 插入菜单
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(1, 0, '系统管理', 'M', NULL, 'system', NULL, 'system', 1, '0', '1', '系统管理目录'),
(2, 1, '用户管理', 'C', 'system:user:list', 'user', 'system/user/index', 'user', 1, '0', '1', '用户管理菜单'),
(3, 1, '角色管理', 'C', 'system:role:list', 'role', 'system/role/index', 'peoples', 2, '0', '1', '角色管理菜单'),
(4, 1, '菜单管理', 'C', 'system:menu:list', 'menu', 'system/menu/index', 'tree-table', 3, '0', '1', '菜单管理菜单'),
(5, 0, '内容管理', 'M', NULL, 'content', NULL, 'catalog', 2, '0', '1', '内容管理目录'),
(6, 5, '文章管理', 'C', 'content:article:list', 'article', 'content/article/index', 'catalog', 1, '0', '1', '文章管理菜单'),
(7, 1, '系统配置', 'C', 'system:config:list', 'config', 'system/config/index', 'setting', 4, '0', '1', '系统配置菜单'),
(8, 0, '监控管理', 'M', NULL, 'monitor', NULL, 'monitor', 3, '0', '1', '系统监控目录'),
(9, 8, '登录日志', 'C', 'monitor:logininfor:list', 'logininfor', 'monitor/logininfor/index', 'logininfor', 1, '0', '1', '登录日志菜单'),
(10, 8, '定时任务', 'C', 'monitor:job:list', 'jobs', 'monitor/jobs/index', 'job', 2, '0', '1', '定时任务菜单'),
(11, 0, '工具管理', 'M', NULL, 'tool', NULL, 'tool', 4, '0', '1', '系统工具目录'),
(12, 11, '代码生成', 'C', 'tool:gen:list', 'gen', 'tool/gen/index', 'code', 1, '0', '1', '代码生成菜单'),
(13, 8, '数据备份', 'C', 'system:backup:list', 'backup', 'monitor/backup/index', 'backup', 3, '0', '1', '数据备份菜单');

-- 关联：admin 用户拥有 admin 角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 关联：admin 角色拥有所有菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13);

-- 初始化一条公告数据
INSERT IGNORE INTO `sys_notice` (`id`, `notice_title`, `notice_content`, `notice_type`, `status`, `scope`) VALUES
(1, '欢迎使用 BareZen', '<p>BareZen 是一个现代化的企业管理平台。</p><p>如需帮助请联系系统管理员。</p>', '2', '1', 'public');

-- 字典类型
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`) VALUES
(1, '用户性别', 'sys_user_sex', '1', '用户性别列表'),
(2, '菜单状态', 'sys_show_hide', '1', '菜单状态列表'),
(3, '系统开关', 'sys_normal_disable', '1', '系统开关列表'),
(4, '通知类型', 'sys_notice_type', '1', '通知公告类型'),
(5, '操作类型', 'sys_oper_type', '1', '操作日志操作类型');

-- 字典数据：用户性别
INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `remark`) VALUES
(1, 1, '男', '0', 'sys_user_sex', 'primary', 'Y', '1', '男性'),
(2, 2, '女', '1', 'sys_user_sex', 'danger', 'N', '1', '女性'),
(3, 3, '未知', '2', 'sys_user_sex', 'info', 'N', '1', '未知性别');

-- 字典数据：菜单状态
INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `remark`) VALUES
(4, 1, '显示', '0', 'sys_show_hide', 'danger', 'Y', '1', '菜单显示'),
(5, 2, '隐藏', '1', 'sys_show_hide', 'warning', 'N', '1', '菜单隐藏');

-- 字典数据：系统开关
INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `remark`) VALUES
(6, 1, '正常', '1', 'sys_normal_disable', 'primary', 'Y', '1', '正常'),
(7, 2, '停用', '0', 'sys_normal_disable', 'danger', 'N', '1', '停用');

-- 字典数据：通知类型
INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `remark`) VALUES
(8, 1, '通知', '1', 'sys_notice_type', 'warning', 'Y', '1', '弹窗通知'),
(9, 2, '公告', '2', 'sys_notice_type', 'info', 'Y', '1', '首页公告');

-- 字典数据：操作类型
INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `remark`) VALUES
(10, 1, '新增', '1', 'sys_oper_type', 'success', 'Y', '1', '新增操作'),
(11, 2, '修改', '2', 'sys_oper_type', 'warning', 'Y', '1', '修改操作'),
(12, 3, '删除', '3', 'sys_oper_type', 'danger', 'Y', '1', '删除操作'),
(13, 4, '授权', '4', 'sys_oper_type', 'primary', 'Y', '1', '授权操作'),
(14, 5, '导出', '5', 'sys_oper_type', 'success', 'Y', '1', '导出操作'),
(15, 6, '导入', '6', 'sys_oper_type', 'success', 'Y', '1', '导入操作'),
(16, 7, '强退', '7', 'sys_oper_type', 'danger', 'Y', '1', '强退操作'),
(17, 8, '清空数据', '8', 'sys_oper_type', 'danger', 'Y', '1', '清空数据操作'),
(18, 9, '其他', '0', 'sys_oper_type', 'info', 'Y', '1', '其他操作');

-- 文章分类
INSERT IGNORE INTO `art_category` (`id`, `name`, `description`, `order_num`, `status`) VALUES
(1, '技术', '技术文章、框架解析、踩坑记录', 1, 1),
(2, '产品', '产品设计、需求分析、用户体验', 2, 1),
(3, '设计', 'UI设计、UX研究、设计系统', 3, 1),
(4, '运营', '运营策略、数据分析、增长黑客', 4, 1);

-- 系统配置（密码策略）
INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `status`, `remark`) VALUES
(1, '密码最小长度', 'sys.account.password_min_length', '6', 'Y', '1', '密码最小长度限制'),
(2, '密码错误锁定次数', 'sys.account.lock_max_attempts', '5', 'Y', '1', '登录失败最大次数'),
(3, '密码错误锁定时间(分钟)', 'sys.account.lock_duration', '15', 'Y', '1', '锁定持续时间'),
(4, '用户注册开关', 'sys.account.register_user', 'false', 'Y', '1', '是否允许注册账号'),
(5, 'Token 过期时间(毫秒)', 'sys.auth.token_expiration', '86400000', 'Y', '1', 'JWT Token过期时间');

-- 自增起始值（避免与雪花算法冲突）
ALTER TABLE `sys_user` AUTO_INCREMENT = 10000;
ALTER TABLE `sys_role` AUTO_INCREMENT = 10000;
ALTER TABLE `sys_menu` AUTO_INCREMENT = 10000;
