-- ============================================
-- BareZen_Base V24: 密码策略与注册开关调整
-- ============================================
-- 1. 密码最小长度恢复为 8 位（强密码：大小写字母+数字+特殊字符，
--    与 PasswordUtil.validatePasswordPolicy 保持一致）
-- 2. 确认注册功能关闭（系统统一由管理员批量创建账号）

UPDATE `sys_config`
SET `config_value` = '8', `remark` = '密码最小长度限制（8位，需含大小写字母、数字与特殊字符）'
WHERE `config_key` = 'sys.account.password_min_length';

UPDATE `sys_config`
SET `config_value` = 'false', `remark` = '是否允许注册账号（关闭，统一管理员建号）'
WHERE `config_key` = 'sys.account.register_user';
