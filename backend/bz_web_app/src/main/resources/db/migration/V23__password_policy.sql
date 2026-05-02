-- ============================================
-- BareZen_Base V23: 密码强度策略调整
-- ============================================
-- 测试/内部环境密码最小长度由 8 位调整为 6 位，
-- 与 AuthController / SysUserServiceImpl 校验逻辑保持一致。

UPDATE `sys_config`
SET `config_value` = '6', `remark` = '密码最小长度限制（6位，含字母和数字）'
WHERE `config_key` = 'sys.account.password_min_length';
