-- 工资管理插件数据库迁移脚本
-- Flyway版本: V47
-- 插件ID: com.linan.plugin.salary

-- 工资结构表
CREATE TABLE IF NOT EXISTS salary_structure (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工资结构ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    user_name VARCHAR(50) COMMENT '员工姓名',
    base_salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '基本工资',
    performance_salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '绩效工资',
    position_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '岗位津贴',
    transport_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '交通补贴',
    meal_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '餐补',
    other_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '其他补贴',
    social_insurance_personal DECIMAL(10,2) DEFAULT 0.00 COMMENT '社保个人缴纳',
    housing_fund_personal DECIMAL(10,2) DEFAULT 0.00 COMMENT '公积金个人缴纳',
    income_tax DECIMAL(10,2) DEFAULT 0.00 COMMENT '个税',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资结构表';

-- 工资发放记录表
CREATE TABLE IF NOT EXISTS salary_payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '发放记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    user_name VARCHAR(50) COMMENT '员工姓名',
    payment_month VARCHAR(7) NOT NULL COMMENT '发放月份(YYYY-MM)',
    base_salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '基本工资',
    performance_salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '绩效工资',
    position_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '岗位津贴',
    transport_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '交通补贴',
    meal_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '餐补',
    other_allowance DECIMAL(10,2) DEFAULT 0.00 COMMENT '其他补贴',
    total_income DECIMAL(10,2) DEFAULT 0.00 COMMENT '总收入',
    social_insurance_personal DECIMAL(10,2) DEFAULT 0.00 COMMENT '社保个人缴纳',
    housing_fund_personal DECIMAL(10,2) DEFAULT 0.00 COMMENT '公积金个人缴纳',
    income_tax DECIMAL(10,2) DEFAULT 0.00 COMMENT '个税',
    total_deduction DECIMAL(10,2) DEFAULT 0.00 COMMENT '总扣除',
    actual_salary DECIMAL(10,2) DEFAULT 0.00 COMMENT '实发工资',
    payment_status VARCHAR(1) DEFAULT '0' COMMENT '发放状态: 0=未发放, 1=已发放',
    payment_time DATETIME COMMENT '发放时间',
    remark VARCHAR(500) COMMENT '备注',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_user_month (user_id, payment_month),
    KEY idx_payment_month (payment_month),
    KEY idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资发放记录表';

-- 插入工资管理菜单（V1 sys_menu 表结构：无 is_frame/is_cache 列）
-- 菜单 ID 从 10031 起：10019 已被 V40 资产分类占用、10020 已被 V42 知识库分类占用、
-- 10021-10030 已部分用于其他业务菜单；10031-10259 段空闲，避免 INSERT IGNORE 静默跳过。
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10031, 0, '工资管理', 'M', NULL, 'salary', NULL, 'money-circle', 10, '0', '0', '工资管理模块'),
(10032, 10031, '工资结构', 'C', 'salary:structure:list', 'structure', 'salary/structure/index', 'edit', 1, '0', '0', '工资结构管理'),
(10033, 10031, '工资发放', 'C', 'salary:payment:list', 'payment', 'salary/payment/index', 'upload', 2, '0', '0', '工资发放管理'),
(10034, 10031, '工资统计', 'C', 'salary:statistics:view', 'statistics', 'salary/statistics/index', 'bar-chart', 3, '0', '0', '工资统计报表');

-- 插入工资管理按钮权限
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10035, 10032, '工资结构查询', 'F', 'salary:structure:list', '', '', '#', 1, '0', '0', ''),
(10036, 10032, '工资结构新增', 'F', 'salary:structure:add', '', '', '#', 2, '0', '0', ''),
(10037, 10032, '工资结构修改', 'F', 'salary:structure:edit', '', '', '#', 3, '0', '0', ''),
(10038, 10032, '工资结构删除', 'F', 'salary:structure:delete', '', '', '#', 4, '0', '0', ''),
(10039, 10032, '工资结构计算', 'F', 'salary:structure:calculate', '', '', '#', 5, '0', '0', ''),
(10040, 10033, '工资发放查询', 'F', 'salary:payment:list', '', '', '#', 1, '0', '0', ''),
(10041, 10033, '工资发放操作', 'F', 'salary:payment:execute', '', '', '#', 2, '0', '0', ''),
(10042, 10034, '工资统计查看', 'F', 'salary:statistics:view', '', '', '#', 1, '0', '0', '');