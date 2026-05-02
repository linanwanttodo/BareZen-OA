-- ============================================
-- BareZen_Base V17: 工作日志（日报/周报）
-- ============================================
-- 公司内部管理系统：员工填写日报/周报，上级可查看并批阅。

-- 1. 工作日志表
CREATE TABLE IF NOT EXISTS `hr_work_log` (
  `id` BIGINT NOT NULL COMMENT '主键ID（雪花ID）',
  `user_id` BIGINT NOT NULL COMMENT '作者用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '作者姓名（冗余）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '作者部门（冗余）',
  `log_type` VARCHAR(10) NOT NULL DEFAULT 'daily' COMMENT '日志类型：daily=日报 weekly=周报',
  `log_date` VARCHAR(20) DEFAULT NULL COMMENT '日志日期（yyyy-MM-dd，周报填周结束日）',
  `content` TEXT COMMENT '日志内容',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '批阅人用户ID',
  `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT '批阅人姓名',
  `review_time` VARCHAR(20) DEFAULT NULL COMMENT '批阅时间',
  `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '批阅意见',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者ID',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者ID',
  `del_flag` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_log_date` (`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日志表';

-- 2. 工作日志菜单（id=10012，挂载在人事管理目录 id=10001 下）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10012, 10001, '工作日志', 'C', 'hr:worklog:list', 'hr/worklog', 'hr/worklog/index', 'book', 5, '0', '1', '工作日志菜单');

-- 3. F 类型按钮权限
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `icon`, `order_num`, `visible`, `status`, `remark`) VALUES
(10237, 10012, '日志新增', 'F', 'hr:worklog:add', NULL, NULL, '#', 1, '0', '1', '日志新增按钮'),
(10238, 10012, '日志修改', 'F', 'hr:worklog:edit', NULL, NULL, '#', 2, '0', '1', '日志修改按钮'),
(10239, 10012, '日志删除', 'F', 'hr:worklog:delete', NULL, NULL, '#', 3, '0', '1', '日志删除按钮'),
(10240, 10012, '日志批阅', 'F', 'hr:worklog:review', NULL, NULL, '#', 4, '0', '1', '日志批阅按钮');

-- 4. admin 角色授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 10012),
(1, 10237), (1, 10238), (1, 10239), (1, 10240);
