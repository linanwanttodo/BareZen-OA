-- ============================================
-- BareZen_Base V7: 补充初始数据（部门、用户-部门关联、文件管理示例）
-- ============================================
-- 创建此迁移的原因是：原先应用的 schema.sql 中包含以下数据 INSERT，
-- 但 schema.sql 由 spring.sql.init.mode 控制（已设为 never 以禁用此功能），
-- 导致这些数据从未被执行，sys_dept/sys_user_dept/sys_file 表始终为空。
-- 现已全部迁移至 Flyway 管理，确保一致性。

-- 部门数据（根部门：总公司）
INSERT IGNORE INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `sort`, `leader`, `phone`, `email`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, 0, '总公司', 1, '超级管理员', '13888888888', 'admin@barezen.com', '0', NOW(), NOW(), NULL, NULL, 0);

-- 用户-部门关联：admin 用户关联到总公司，角色为管理员
INSERT IGNORE INTO `sys_user_dept` (`user_id`, `dept_id`, `role_type`) VALUES (1, 1, '1');

-- 文件管理根目录示例（admin 范围的根文件夹）
INSERT IGNORE INTO `sys_file` (`id`, `file_name`, `original_name`, `file_path`, `file_url`, `file_size`, `file_type`, `storage_type`, `is_folder`, `folder_id`, `scope`, `sort_order`, `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`)
VALUES (1, '默认文件夹', '默认文件夹', '', NULL, 0, 'folder', 'local', 1, 0, 'admin', 0, NOW(), NOW(), 1, 1, 0);

-- 同步更新 sys_config 中的密码最小长度（与 AuthController 的 8 位策略保持一致）
UPDATE `sys_config` SET `config_value` = '8', `remark` = '密码最小长度限制（8位，含字母和数字）' WHERE `config_key` = 'sys.account.password_min_length';